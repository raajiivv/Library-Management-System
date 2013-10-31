package edu.sjsu.cmpe.library.repository;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;






import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.fusesource.stomp.jms.message.StompJmsMessage;

import edu.sjsu.cmpe.library.config.LibraryServiceConfiguration;
import edu.sjsu.cmpe.library.domain.Book;
import edu.sjsu.cmpe.library.domain.Book.Status;

public class BookRepository implements BookRepositoryInterface {
    /** In-memory map to store books. (Key, Value) -> (ISBN, Book) */
    private final ConcurrentHashMap<Long, Book> bookInMemoryMap;
    private final LibraryServiceConfiguration configuration;

    /** Never access this key directly; instead use generateISBNKey() */
    private long isbnKey;
	private String apolloUser;
	private String apolloPassword;
	private String apolloHost;
	private int apolloPort;
	private String stompQueue;
	private String stompTopic;

    public BookRepository(LibraryServiceConfiguration config) {
	bookInMemoryMap = seedData();
	this.configuration = config;
	System.out.println("checkpoint");
	apolloUser = configuration.getApolloUser();
	apolloPassword = configuration.getApolloPassword();
	apolloHost = configuration.getApolloHost();
	apolloPort = configuration.getApolloPort();
	stompQueue = configuration.getStompQueueName();
	stompTopic = configuration.getStompTopicName();
	System.out.println(apolloUser);


	isbnKey = 2;
    }

    private ConcurrentHashMap<Long, Book> seedData(){
	ConcurrentHashMap<Long, Book> bookMap = new ConcurrentHashMap<Long, Book>();
	Book book = new Book();
	book.setIsbn(1);
	book.setCategory("computer");
	book.setTitle("Java Concurrency in Practice");
	try {
	    book.setCoverimage(new URL("http://goo.gl/N96GJN"));
	} catch (MalformedURLException e) {
	    // eat the exception
	}
	bookMap.put(book.getIsbn(), book);

	book = new Book();
	book.setIsbn(2);
	book.setCategory("computer");
	book.setTitle("Restful Web Services");
	try {
	    book.setCoverimage(new URL("http://goo.gl/ZGmzoJ"));
	} catch (MalformedURLException e) {
	    // eat the exception
	}
	bookMap.put(book.getIsbn(), book);

	return bookMap;
    }

    /**
     * This should be called if and only if you are adding new books to the
     * repository.
     * 
     * @return a new incremental ISBN number
     */
    private final Long generateISBNKey() {
	// increment existing isbnKey and return the new value
	return Long.valueOf(++isbnKey);
    }

    /**
     * This will auto-generate unique ISBN for new books.
     */
    @Override
    public Book saveBook(Book newBook) {
	checkNotNull(newBook, "newBook instance must not be null");
	// Generate new ISBN
	Long isbn = generateISBNKey();
	newBook.setIsbn(isbn);
	// TODO: create and associate other fields such as author

	// Finally, save the new book into the map
	bookInMemoryMap.putIfAbsent(isbn, newBook);

	return newBook;
    }

    /**
     * @see edu.sjsu.cmpe.library.repository.BookRepositoryInterface#getBookByISBN(java.lang.Long)
     */
    @Override
    public Book getBookByISBN(Long isbn) {
	checkArgument(isbn > 0,
		"ISBN was %s but expected greater than zero value", isbn);
	return bookInMemoryMap.get(isbn);
    }

    @Override
    public List<Book> getAllBooks() {
	return new ArrayList<Book>(bookInMemoryMap.values());
    }

    /*
     * Delete a book from the map by the isbn. If the given ISBN was invalid, do
     * nothing.
     * 
     * @see
     * edu.sjsu.cmpe.library.repository.BookRepositoryInterface#delete(java.
     * lang.Long)
     */
    @Override
    public void delete(Long isbn) {
	bookInMemoryMap.remove(isbn);
    }
    
    @Override
    	public Book updateBookStatus(Book book, Status tempStatus) throws JMSException{
   		book.setStatus(tempStatus); 
   		System.out.println("checkpint2");
   		String libname;
   		if(configuration.getStompTopicName().equals("/topic/05452.book.*"))
   			libname = "library-a";
   		else
   			libname = "library-b";
   		producer(libname+":"+book.getIsbn());
   		System.out.println("checkpint3");
    	return book;
    }
    
    public void producer(String tempMsg) throws JMSException{
    	System.out.println("checkpint4");
    	StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
    	factory.setBrokerURI("tcp://" + apolloHost + ":" + apolloPort);
    	System.out.println(factory.getBrokerURI());

    	Connection connection = factory.createConnection(apolloUser, apolloPassword);
    	connection.start();
    	Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    	Destination dest = new StompJmsDestination(stompQueue);
    	MessageProducer producer = session.createProducer(dest);

    	TextMessage msg = session.createTextMessage(tempMsg);
    	msg.setLongProperty("id", System.currentTimeMillis());
    	System.out.println(msg.getText());
    	//}
    	producer.send(msg);
    	//producer.send(session.createTextMessage("SHUTDOWN"));
    	connection.close();

        }
    
    public void listener() throws JMSException {
    	StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
    	factory.setBrokerURI("tcp://" + apolloHost + ":" + apolloPort);

    	Connection connection = factory.createConnection(apolloUser, apolloPassword);
    	connection.start();
    	Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    	Destination dest = new StompJmsDestination(stompTopic);

    	MessageConsumer consumer = session.createConsumer(dest);
    	System.currentTimeMillis();
    	System.out.println("Waiting for messages...");
    	while(true) {
    	    Message msg = consumer.receive();
    	    if( msg instanceof  TextMessage ) {
    		String body = ((TextMessage) msg).getText();
    		if( "SHUTDOWN".equals(body)) {
    		    break;
    		}
    		System.out.println("Received message = " + body);

    	    } else if (msg instanceof StompJmsMessage) {
    		StompJmsMessage smsg = ((StompJmsMessage) msg);
    		String body = smsg.getFrame().contentAsString();
    		if ("SHUTDOWN".equals(body)) {
    		    break;
    		}
    		System.out.println("Received message = " + body);

    	    } else {
    		System.out.println("Unexpected message type: "+msg.getClass());
    	    }
    	}
    	connection.close();

    }



}
