import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class serv
{
  // A pre-allocated buffer for the received data
  static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );

  // Decoder for incoming text -- assume UTF-8
    static private final Charset charset = Charset.forName("UTF8");
    static private final CharsetDecoder decoder = charset.newDecoder();
    static private final CharsetEncoder encoder = charset.newEncoder();

    static public Set <String> usednames = new HashSet<String>();
	
    static public void main( String args[] ) throws Exception {
    // Parse port from command line
    int port = Integer.parseInt( args[0] );
    try {
      // Instead of creating a ServerSocket, create a ServerSocketChannel
      ServerSocketChannel ssc = ServerSocketChannel.open();

      // Set it to non-blocking, so we can use select
      ssc.configureBlocking( false );

      // Get the Socket connected to this channel, and bind it to the
      // listening port
      ServerSocket ss = ssc.socket();
      InetSocketAddress isa = new InetSocketAddress( port );
      ss.bind( isa );

      // Create a new Selector for selecting
      Selector selector = Selector.open();

      // Register the ServerSocketChannel, so we can listen for incoming
      // connections
      ssc.register( selector, SelectionKey.OP_ACCEPT );
      System.out.println( "Listening on port "+port );

      while (true) {
        // See if we've had any activity -- either an incoming connection,
        // or incoming data on an existing connection
        int num = selector.select();

        // If we don't have any activity, loop around and wait again
        if (num == 0) {
          continue;
        }

        // Get the keys corresponding to the activity that has been
        // detected, and process them one by one
        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> it = keys.iterator();
        while (it.hasNext()) {
          // Get a key representing one of bits of I/O activity
          SelectionKey key = it.next();

          // What kind of activity is it?
          if ((key.readyOps() & SelectionKey.OP_ACCEPT) ==
            SelectionKey.OP_ACCEPT) {

            // It's an incoming connection.  Register this socket with
            // the Selector so we can listen for input on it
            Socket s = ss.accept();
            System.out.println( "Got connection from "+s );

            // Make sure to make it non-blocking, so we can use a selector
            // on it.
            SocketChannel sc = s.getChannel();
            sc.configureBlocking( false );

	    user obj = new user();


	    // Register it with the selector, for reading
            sc.register( selector, SelectionKey.OP_READ, obj);

	    
          } else if ((key.readyOps() & SelectionKey.OP_READ) ==
            SelectionKey.OP_READ) {

            SocketChannel sc = null;
            try {

              // It's incoming data on a connection -- process it
              sc = (SocketChannel)key.channel();
	      boolean ok = processInput( sc, selector, key);

              // If the connection is dead, remove it from the selector
              // and close it
              if (!ok) {
                key.cancel();

                Socket s = null;
                try {
                  s = sc.socket();
                  System.out.println( "Closing connection to "+s );
                  s.close();
                } catch( IOException ie ) {
                  System.err.println( "Error closing socket "+s+": "+ie );
                }
              }

            } catch( IOException ie ) {

              // On exception, remove this channel from the selector
              key.cancel();

              try {
                sc.close();
              } catch( IOException ie2 ) { System.out.println( ie2 ); }

              System.out.println( "Closed "+sc );
            }
          }
        }

        // We remove the selected keys, because we've dealt with them.
        keys.clear();
      }
    } catch( IOException ie ) {
      System.err.println( ie );
    }
  }


  // Just read the message from the socket and send it to stdout
    static private boolean processInput( SocketChannel sc, Selector selector, SelectionKey key) throws IOException {
    // Read the message to the buffer
    
	buffer.clear();
	sc.read( buffer );
	buffer.flip();

	user clint = (user)key.attachment();
	// Decode the message
	String message = decoder.decode(buffer).toString();
	clint.addbuff(message);
	String cur = clint.getbuffer();
	while(cur.contains("\n"))
	    {
		String[] msspl = cur.split("\n", 2);
		cur = msspl[1];
		System.out.println("o cur e :" + cur + ".");
		String newmss = msspl[0];
		if(handle(newmss, selector, key))
		    return false;
	    }
	clint.addbuff(cur);
	return true;
    }

    static private boolean handle(String message, Selector selector, SelectionKey key) throws IOException{
	if(message.charAt(0) != '/' || message.charAt(1) == '/')
	    {
		user clint = (user)key.attachment();
	        if(clint.getst() != 2)
		    {
			SocketChannel sc = (SocketChannel)key.channel();
			sc.write(encoder.encode(CharBuffer.wrap("ERROR\n")));
		        return false;
		    }
		if(message.charAt(0) == '/')
			message = message.substring(1);
		String nm = "MESSAGE " + clint.getnick() + " " + message + "\n";
		Set<SelectionKey> keys = selector.keys();
		Iterator<SelectionKey> keyIterator = keys.iterator();
		while(keyIterator.hasNext())
		    {
			SelectionKey key1 = keyIterator.next();
			if(key1.isAcceptable())
			    continue;
			SocketChannel sc1 = (SocketChannel)key1.channel();
			user clint2 = (user)key1.attachment();
			if(clint2.getst() != 2 || clint2.getroom().compareTo(clint.getroom()) != 0)
			    continue;
			sc1.write(encoder.encode(CharBuffer.wrap(nm)));
		    }
		return false;
	    }
	else
	    {
		SocketChannel sc = (SocketChannel)key.channel();
		String parts[] = message.split(" ",3);		
		user clint = (user)key.attachment();
		Set<SelectionKey> keys = selector.keys();
		Iterator<SelectionKey> keyIterator = keys.iterator();
		switch (parts[0]) {
		case "/nick":
		    if(parts.length!=2)
		    {
			sc.write(encoder.encode(CharBuffer.wrap("ERROR\n")));
			return false;
		    }
		    if(usednames.contains(parts[1]))
			{
			    sc.write(encoder.encode(CharBuffer.wrap("ERROR\n")));
			    return false;
			}
		    switch (clint.getst()) {
		    case 0:
			clint.state=1;
			break;
		    case 2:
			keyIterator = keys.iterator();
			while(keyIterator.hasNext())
			    {
				SelectionKey key1 = keyIterator.next();
				user clint2 = (user)key1.attachment();
				if(key1.isAcceptable() || (clint2.getst()==2 && clint2.getroom().compareTo(clint.room)!=0))
				    continue;
				SocketChannel sc1 = (SocketChannel)key1.channel();
				sc1.write(encoder.encode(CharBuffer.wrap("NEWNICK "+clint.getnick()+" "+parts[1]+"\n")));
			    }
			usednames.remove(clint.getnick());
			break;
		    default:
			usednames.remove(clint.getnick());
			break;
		    }
		    sc.write(encoder.encode(CharBuffer.wrap("OK\n")));
		    clint.newnick(parts[1]);
		    usednames.add(parts[1]);
		    return false;
		case "/join":
		    if(parts.length!=2)
		    {
			sc.write(encoder.encode(CharBuffer.wrap("ERROR\n")));
			return false;
		    }
		    switch (clint.getst()) {
		    case 1:
			keyIterator = keys.iterator();
			while(keyIterator.hasNext())
			    {
				SelectionKey key1 = keyIterator.next();
				user clint2 = (user)key1.attachment();
				if(key1.isAcceptable() ||
				   key==key1 ||
				   clint2.getst()!=2 ||
				   (clint2.getst()==2 && clint2.getroom().compareTo(parts[1])!=0))
				    continue;
				SocketChannel sc1 = (SocketChannel)key1.channel();
				sc1.write(encoder.encode(CharBuffer.wrap("JOINED "+clint.getnick()+"\n")));
			    }
			break;
		    case 2:
			keyIterator = keys.iterator();
			while(keyIterator.hasNext())
			    {
				SelectionKey key1 = keyIterator.next();
				user clint2 = (user)key1.attachment();
				if(key1.isAcceptable() || key==key1)
				    continue;
				if(clint2.getst()==2 && clint2.getroom().compareTo(clint.room)==0)
				    {
					SocketChannel sc1 = (SocketChannel)key1.channel();
					sc1.write(encoder.encode(CharBuffer.wrap("JOINED "+clint.getnick()+"\n")));
				    }
				else if(clint2.getst()==2 && clint2.getroom().compareTo(parts[1])==0)
				    {
					SocketChannel sc1 = (SocketChannel)key1.channel();
					sc1.write(encoder.encode(CharBuffer.wrap("LEFT "+clint.getnick()+"\n")));
				    }
			    }
			break;
		    default:
			sc.write(encoder.encode(CharBuffer.wrap("ERROR\n")));
			return false;
		    }
		    sc.write(encoder.encode(CharBuffer.wrap("OK\n")));
		    clint.changeroom(parts[1]);
		    return false;
		case "/leave":
		    if(parts.length!=1)
		    {
			sc.write(encoder.encode(CharBuffer.wrap("ERROR\n")));
			return false;
		    }
		    switch(clint.getst()) {
		    case 2:
			keyIterator = keys.iterator();
			while(keyIterator.hasNext())
			    {
				SelectionKey key1 = keyIterator.next();
				user clint2 = (user)key1.attachment();
				if(key1.isAcceptable() ||
				   key==key1 ||
				   clint2.getst()==2 ||
				   (clint2.getst()==2 && clint2.getroom().compareTo(clint.getroom())!=0))
				    continue;
				SocketChannel sc1 = (SocketChannel)key1.channel();
				sc1.write(encoder.encode(CharBuffer.wrap("LEFT "+clint.getnick()+"\n")));
			    }
			break;
		    default:
			sc.write(encoder.encode(CharBuffer.wrap("ERROR\n")));
			return false;
		    }
		    sc.write(encoder.encode(CharBuffer.wrap("OK\n")));
		    clint.leaveroom();
		    return false;
		case "/bye":
		    if(parts.length!=1)
			{
			    sc.write(encoder.encode(CharBuffer.wrap("ERROR\n")));
			    return false;
			}
		    switch(clint.getst()) {
		    case 2:
			keyIterator = keys.iterator();
			while(keyIterator.hasNext())
			    {
				SelectionKey key1 = keyIterator.next();
				user clint2 = (user)key1.attachment();
				if(key1.isAcceptable() ||
				   key==key1 ||
				   clint2.getst()!=2 ||
				   (clint2.getst()==2 && clint2.getroom().compareTo(clint.getroom())!=0))
				    continue;
				SocketChannel sc1 = (SocketChannel)key1.channel();
				sc1.write(encoder.encode(CharBuffer.wrap("LEFT "+clint.getnick()+"\n")));
			    }
			break;
		    default:
			break;
		    }
		    usednames.remove(clint.getnick());
		    sc.write(encoder.encode(CharBuffer.wrap("BYE\n")));
		    return true;
		case "/priv":
		    if(parts.length!=3 || !usednames.contains(parts[1]))
			{
			    sc.write(encoder.encode(CharBuffer.wrap("ERROR\n")));
			    return false;
			}
		    sc.write(encoder.encode(CharBuffer.wrap("OK\n")));
		    if(message.charAt(0) == '/')
			message = message.substring(1);
		    String nm = "PRIVATE " + clint.getnick() + " " + parts[2] + "\n";
		    while(keyIterator.hasNext())
			{
			    SelectionKey key1 = keyIterator.next();
			    user clint2 = (user)key1.attachment();
			    if(key1.isAcceptable() || clint2.getnick().compareTo(parts[1])!=0)
				continue;
			    SocketChannel sc1 = (SocketChannel)key1.channel();
			    sc1.write(encoder.encode(CharBuffer.wrap(nm)));
			}
		    return false;
		default:
		    sc.write(encoder.encode(CharBuffer.wrap("ERROR\n")));
		    return false;
		}
	    }
    }
}
