package com.velocitypowered.proxy;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftPipelineUtils;
import com.velocitypowered.proxy.connection.client.HandshakeSessionHandler;
import com.velocitypowered.proxy.protocol.packets.EncryptionRequest;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public class VelocityServer {
    private static VelocityServer server;

    private EventLoopGroup bossGroup;
    private EventLoopGroup childGroup;
    private KeyPair serverKeyPair;

    public VelocityServer() {

    }

    public static VelocityServer getServer() {
        return server;
    }

    public KeyPair getServerKeyPair() {
        return serverKeyPair;
    }

    public void initialize() {
        // Create a key pair
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            serverKeyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to generate server encryption key", e);
        }

        // Start the listener
        bossGroup = new NioEventLoopGroup();
        childGroup = new NioEventLoopGroup();
        server = this;
        new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .group(bossGroup, childGroup)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        MinecraftPipelineUtils.strapPipelineForProxy(ch);

                        MinecraftConnection connection = new MinecraftConnection(ch);
                        connection.setState(StateRegistry.HANDSHAKE);
                        connection.setSessionHandler(new HandshakeSessionHandler(connection));
                        ch.pipeline().addLast("handler", connection);
                    }
                })
                .bind(26671)
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            System.out.println("Listening on " + future.channel().localAddress());
                        } else {
                            System.out.println("Can't bind to " + future.channel().localAddress());
                            future.cause().printStackTrace();
                        }
                    }
                });
    }

    public Bootstrap initializeGenericBootstrap() {
        return new Bootstrap()
                .channel(NioSocketChannel.class)
                .group(childGroup);
    }
}
