package top.yumbo.ai.rag.api.server;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.LogMessageProvider;

/**
 * Netty HTTP 服务器封装 (Netty HTTP server wrapper)
 *
 * 这是项目的轻量 HTTP 服务器实现，用于将 HTTP 请求分发到应用的请求处理器。
 * (This is a lightweight HTTP server implementation used to dispatch HTTP requests
 *  to the application's request handler.)
 */
@Slf4j
public class NettyHttpServer {
    private final int port;
    private final HttpRequestHandler requestHandler;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    public NettyHttpServer(int port, HttpRequestHandler requestHandler) {
        this.port = port;
        this.requestHandler = requestHandler;
    }
    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                            .addLast(new HttpServerCodec())
                            .addLast(new HttpObjectAggregator(65536))
                            .addLast(requestHandler);
                    }
                });
            channelFuture = bootstrap.bind(port).sync();
            log.info(LogMessageProvider.getMessage("log.http.started", port));
        } catch (Exception e) {
            log.error(LogMessageProvider.getMessage("log.http.start_failed"), e);
            shutdown();
            throw e;
        }
    }
    public void shutdown() {
        if (channelFuture != null) channelFuture.channel().close();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        log.info(LogMessageProvider.getMessage("log.http.shutdown"));
    }
}
