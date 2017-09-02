package com.paul0523;


import java.io.BufferedReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TcpServer {

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private SelectionKey selectionKey;
    private Map<SelectionKey, String> cache = new HashMap<>();

    public static void main(String[] args) throws Exception{
        new TcpServer().start();
    }

    public TcpServer() throws Exception {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8080));
        serverSocketChannel.configureBlocking(false);
        selector = Selector.open();
        selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void start() throws Exception{

        while (true) {
            selector.select();
            for (Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext(); ) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key);
                } else if (key.isReadable()) {
                    handleRead(key);
                } else if (key.isWritable()) {
                    handleWrite(key);
                } else if (key.isConnectable()) {
                    handleConnet(key);
                }
                iterator.remove();

            }
        }

    }

    private void handleConnet(SelectionKey key) throws Exception {
        System.out.println("建立连接");
    }

    private void handleWrite(SelectionKey key) throws Exception {
        System.out.println("向通道写数据。。。");
        ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
        SocketChannel socketChannel = (SocketChannel) key.channel();
        socketChannel.write(byteBuffer);
        key.interestOps(SelectionKey.OP_READ);
    }

    private void handleRead(SelectionKey key) throws Exception {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        System.out.println(cache.containsKey(key));
        ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
        byteBuffer.clear();
        int readbytes = socketChannel.read(byteBuffer);
        byteBuffer.flip();
        if (readbytes > 0) {
            byte[] bytes = new byte[readbytes];
            byteBuffer.get(bytes, 0, readbytes);
            System.out.println(new String(bytes, 0, readbytes, Charset.forName("utf-8")));
        } else if (readbytes == -1) {
            socketChannel.close();
            return;
        }
        byteBuffer.flip();
        key.attach(byteBuffer);
        forCast(byteBuffer);
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void forCast(ByteBuffer byteBuffer) {
        Set<SelectionKey> keys = selector.keys();
        for (SelectionKey key : keys) {
            if (key == selectionKey) {
                continue;
            }
            ByteBuffer newByteBuffer = ByteBuffer.wrap(byteBuffer.array());
            newByteBuffer.position(0);
            newByteBuffer.limit(byteBuffer.limit());
            key.attach(newByteBuffer);
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    private void handleAccept(SelectionKey key) throws Exception {
        SocketChannel socketChannel = ((ServerSocketChannel)key.channel()).accept();
        socketChannel.configureBlocking(false);
        SelectionKey selectionKey = socketChannel.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(1024));
        cache.put(selectionKey, null);
    }

}
