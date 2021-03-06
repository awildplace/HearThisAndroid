package org.sil.hearthis;

import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * SyncServer manages the 'web server' for the synchronization service that supports data
 * exchange with HearThis desktop
 */
public class SyncServer extends Thread
{
    SyncService parent;
    Integer serverPort = 8087;
    private BasicHttpProcessor httpproc = null;
    private BasicHttpContext httpContext = null;
    private HttpService httpService = null;
    private HttpRequestHandlerRegistry registry = null;
    boolean running;

    public SyncServer(SyncService parent)
    {
        super("HearThisAndroidServer");
        this.parent = parent;

        httpproc = new BasicHttpProcessor();
        httpContext = new BasicHttpContext();

        httpproc.addInterceptor(new ResponseDate());
        httpproc.addInterceptor(new ResponseServer());
        httpproc.addInterceptor(new ResponseContent());
        httpproc.addInterceptor(new ResponseConnControl());

        httpService = new HttpService(httpproc,
                new DefaultConnectionReuseStrategy(),
                new DefaultHttpResponseFactory());


        registry = new HttpRequestHandlerRegistry();

        registry.register("*", new DeviceNameHandler(this.parent));
        registry.register("/getfile*", new RequestFileHandler(this.parent));
        registry.register("/putfile*", new AcceptFileHandler(this.parent));
        registry.register("/list*", new ListDirectoryHandler(this.parent));
        httpService.setHandlerResolver(registry);
    }

    public synchronized void startThread()
	{
        running = true;
        super.start();
    }

    // Clear flag so main loop will terminate after next request.
    public synchronized void stopThread()
	{
        running = false;
    }

    // Method executed in thread when super.start() is called.
    @Override
    public void run()
	{
        super.run();

        try
		{
            ServerSocket serverSocket = new ServerSocket(serverPort);

            serverSocket.setReuseAddress(true);

            while(running)
			{
                try
				{
                    final Socket socket = serverSocket.accept();

                    DefaultHttpServerConnection serverConnection = new DefaultHttpServerConnection();

                    serverConnection.bind(socket, new BasicHttpParams());

                    httpService.handleRequest(serverConnection, httpContext);

                    serverConnection.shutdown();
                }
				catch (Exception e)
				{
                    e.printStackTrace();
                }
            }

            serverSocket.close();
        }
        catch (IOException e)
		{
            e.printStackTrace();
        }
    }
}
