package edu.sjsu.cmpe.procurement;

import de.spinscale.dropwizard.jobs.Job;
import de.spinscale.dropwizard.jobs.annotations.Every;

import java.util.ArrayList;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

@Every("300s")
public class ProcurementJobs extends Job {
	private final String apolloUser = "admin";
	private final String apolloPassword = "password";
	private final String apolloHost = "54.215.210.214";
	private final int apolloPort = 61613;
	private final String stompQueue = "/queue/05452.book.orders";
	private final String stompTopic_a = "/topic/05452.book.all";
	private final String stompTopic_b = "/topic/05452.book.computer";

	
	@Override
	public void doJob(){
		//System.out.println("Jobs Working");
		ProcurementJobs jobs = new ProcurementJobs();
		try {
			String lostBooks = jobs.consumer();
			if (!lostBooks.equals("[]"))
				jobs.HttpPOST(lostBooks);
			ArrayList<String> arrivedBooks = new ArrayList<String>();
			arrivedBooks = HttpGET();
			jobs.Publisher(arrivedBooks);
		} catch (Exception e) {
			System.out.println("Unexpected error!");
		}
		
	}

	
public String consumer() throws JMSException, InterruptedException{
	/*String user = "admin";
	String password = "password";
	String host = "54.215.210.214";
	int port = 61613;
	String queue = "/queue/05452.book.orders";
*/	//String destination = queue;

	StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
	factory.setBrokerURI("tcp://" + apolloHost + ":" + apolloPort);
	Connection connection = factory.createConnection(apolloUser, apolloPassword);
	connection.start();
	Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	Destination dest = new StompJmsDestination(stompQueue);
	ArrayList<String> tempString = new ArrayList<String>();
	MessageConsumer consumer = session.createConsumer(dest);
	//System.out.println("Waiting for messages from " + stompQueue + "...");
	while(true) {
		//System.out.println("test1");
	    Message msg = consumer.receive(5000);
	    if(msg==null){
	    	//System.out.println("test2");
	    	break;
	    }
	    if( msg instanceof  TextMessage ) {
	    String body = ((TextMessage) msg).getText();
	    tempString.add(body.split(":")[1]);
		//System.out.println(body.split(":")[1]);
		} 
	    else {
		System.out.println("Unexpected message type: "+msg.getClass());

	    }	    
	}
	String lostIsbn = tempString.toString();
	connection.close();
	return lostIsbn;
	}

public void HttpPOST(String lostBooks){
	
	Client client = Client.create(); 
	//System.out.println(lostBooks);
	String msg = "{\"id\" : \"05452\",\"order_book_isbns\":"+lostBooks+"}";
	WebResource webResource = client.resource("http://54.215.210.214:9000/orders");
	ClientResponse response = webResource.type("application/json").post(ClientResponse.class, msg);
	if (response.getStatus()!=200) {
		throw new RuntimeException("Falied: HTTP error : " +response.getStatus());	
	}
	//System.out.println(response.toString());
	//System.out.println(response.getEntity(String.class));

}

public ArrayList<String> HttpGET() throws JSONException {
	Client client = Client.create();
	WebResource webResource = client.resource("http://54.215.210.214:9000/orders/05452");
	ClientResponse response = webResource.accept("application/json").get(ClientResponse.class);
	if (response.getStatus() != 200) {
		throw new RuntimeException("Failed : HTTP error code : "
				+ response.getStatus());
	}
	//System.out.println(response.toString());
	String output = response.getEntity(String.class);
	//System.out.println(output+"\n");
	JSONObject object = new JSONObject(output);
	JSONArray shipping = object.getJSONArray("shipped_books");
	int n = shipping.length();
	ArrayList<String> arrivedBooks = new ArrayList<String>();
	for (int i =0; i<n ; i++){
		JSONObject getbooks = shipping.getJSONObject(i);
		arrivedBooks.add(getbooks.getLong("isbn")+":\""+getbooks.getString("title")+"\":\""+getbooks.getString("category")+"\":\""+getbooks.getString("coverimage")+"\"");
	}
	//System.out.println(arrivedBooks);
	return arrivedBooks;
}

public void Publisher(ArrayList<String> arrivedBooks) throws JMSException{
	/*String user = "admin";
	String password = "password";
	String host = "54.215.210.214";
	int port = 61613;
	String destination1 = "/topic/05452.book.all";
	String destination2 = "/topic/05452.book.computer";
*/
	StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
	factory.setBrokerURI("tcp://" + apolloHost + ":" + apolloPort);

	Connection connection = factory.createConnection(apolloUser, apolloPassword);
	connection.start();
	Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	Destination dest_lib_a = new StompJmsDestination(stompTopic_a);
	Destination dest_lib_b = new StompJmsDestination(stompTopic_b);
	MessageProducer producer_a = session.createProducer(dest_lib_a);
	MessageProducer producer_b = session.createProducer(dest_lib_b);

	String data;
	for (int i = 0; i < arrivedBooks.size(); i++) {
		data = arrivedBooks.get(i);
		TextMessage msg = session.createTextMessage(data);
		msg.setLongProperty("id", System.currentTimeMillis());
		//System.out.println("message sent to library_a");
		producer_a.send(msg);		
		if (data.split(":")[2].equals("\"computer\"")) {
			//System.out.println("message sent to library_b");
			producer_b.send(msg);
		//System.out.println(data);	
		}
	}
	connection.close();
    }
}