package org.httpclient.sample;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

public class BasicClient {

	private static final String HTTP = "http";
	private static final String HTTPS = "https";
	private static final String UTF8 = "UTF-8";
	
	private static HttpClient httpClient;
	
	private BasicClient() {
	}
	
	private static HttpClient getHttpClient() {
		if (httpClient == null) {
			SchemeRegistry schemeRegistry = new SchemeRegistry();
			schemeRegistry.register(new Scheme(HTTP, 80, PlainSocketFactory.getSocketFactory()));
			schemeRegistry.register(new Scheme(HTTPS, 443, SSLSocketFactory.getSocketFactory()));
			PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager(schemeRegistry);
			connectionManager.setMaxTotal(20);
			connectionManager.setDefaultMaxPerRoute(20);
			
			DefaultHttpClient defaultHttpClient = new DefaultHttpClient(connectionManager, getHttpParams());
			
			HttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(0, false);
			defaultHttpClient.setHttpRequestRetryHandler(retryHandler);
			
			addShutdownHook();
			
			httpClient = defaultHttpClient;
		}
		return httpClient;
	}
	
	private static void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				HttpClientUtils.closeQuietly(httpClient);
			}
		});
	}

	
	private static HttpParams getHttpParams() {
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 2000);
		HttpConnectionParams.setSoTimeout(httpParams, 2000);
		return httpParams;
	}
	
	/**
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public static String execute(HttpUriRequest request) throws Exception {
		try {
			return getHttpClient().execute(request, new ResponseHandler<String>() {
				
				public String handleResponse(HttpResponse response) throws IOException {
					StatusLine statusLine = response.getStatusLine();
					if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
						throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
					}
					HttpEntity entity = response.getEntity();
					if (entity == null) {
						throw new ClientProtocolException("entity may not be null.");
					}
					try {
						return EntityUtils.toString(entity, UTF8);
					} finally {
						EntityUtils.consume(entity);
					}
				}
				
			});
		} catch(IOException e) {
			request.abort();
			throw e;
		}
	}
	
}
