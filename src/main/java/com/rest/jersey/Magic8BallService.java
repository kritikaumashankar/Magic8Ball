package com.rest.jersey;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.ResourceBundle;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@Path("/")
public class Magic8BallService {
	

	//private static ResourceBundle rb = ResourceBundle.getBundle("application");
	private List<Document> answerList = new ArrayList<Document>();
	
	/*Connect to DB*/
	public MongoCollection<Document> connectToDB() throws UnknownHostException {
		System.out.println("Connect to DB");
		MongoClient mongoClient;
		System.out.println(System.getenv().get("MONGODB_URI"));
		if(System.getenv().get("MONGODB_URI")== null)
			 mongoClient = new MongoClient(new MongoClientURI("mongodb://localhost:27017"));
		else
			 mongoClient = new MongoClient(new MongoClientURI(System.getenv().get("MONGODB_URI")));

		//DB database = mongoClient.getDB("MagicBall");
		MongoDatabase db = mongoClient.getDatabase("magicball");
		if(db.getCollection("answers")==null)
			db.createCollection("answers");
		
		MongoCollection<Document> collection = db.getCollection("answers");

		return collection;
	}
	/* GET =  displaying all answers */
	
	@GET
	@Path("/answers")
	@Produces(MediaType.APPLICATION_JSON)
	public String getAllAnswers() throws UnknownHostException {
		 MongoCollection<Document> collection = connectToDB();
		 answerList = collection.find().into(new ArrayList<Document>());
		return answerList.toString();
	}
	
	/* GET = Getting a random answer for a question */
	
	@GET
	@Path("/")
	@Produces(MediaType.TEXT_HTML)
	public String getAnswer(@QueryParam("question") String question) throws UnknownHostException {
		
		//MongoCollection<Document> collection = connectToDB();
		String list=getAllAnswers();
		Random rand = new Random();
		Document doc= (Document) answerList.get(rand.nextInt(answerList.size()));
		return (String) doc.get("answer");
	}
	
	/* CREATE = Adding an answer */
	
	@POST
	@Path("/answers/")
	//@Consumes(MediaType.TEXT_HTML) //specify which MIME media types of representations a resource can accept, or consume, from the client.Request type
	@Produces(MediaType.APPLICATION_JSON) //response type
	public Response addAnswer(@QueryParam("answer") String ans) throws URISyntaxException, UnknownHostException {
		MongoCollection<Document> collection = connectToDB();
		Document document = new Document();
		document.put("answer", ans);
		Document doc= (Document) answerList.stream()
				.filter(d -> d.get("answer").toString().toLowerCase().equals(ans.toLowerCase()))
				.findAny()
				.orElse(null);
		System.out.println(doc);
		if(ans==null || ans.equals("")) {
			return Response.status(400).entity("Please add answer !!").build();
		}
		if(doc != null) {
				return Response.status(401).entity("This answer is already present! Please add different answer!").build();
		}
		
		collection.insertOne(document);
		return Response.created(new URI("/rest/answers/"+document.get( "_id" ))).build();
	}
	
	/* Update = Updating an answer */
	
	@PUT
	@Path("/answers/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateAnswersById(@QueryParam("id") String id, @QueryParam("ans") String ans) throws UnknownHostException {
		
		MongoCollection<Document> collection = connectToDB();
		String list=getAllAnswers();
		System.out.println(answerList.size());
		Document doc= (Document) answerList.stream()
							.filter(d -> d.get("_id").equals(new ObjectId(id)))
							.findAny()
							.orElse(null);
	
		Document newDocument = new Document();
		newDocument.put("_id",new ObjectId(id));
		newDocument.put("answer", ans); 
		
		Document updateObject = new Document();
		updateObject.put("$set", newDocument);
		
		if(ans==null || ans.equals("")) {
			return Response.status(400).entity("Please provide the answer !!").build();
		}

		collection.updateOne(doc, updateObject);

		return Response.ok().entity("Answer updated successfully!!").build();
	}
	
	
	/* DELETE = Deleting an answer */
	@DELETE
	@Path("/answers/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteAnswerById(@QueryParam("id") String id) throws UnknownHostException {
		MongoCollection<Document> collection = connectToDB();
		String list=getAllAnswers();
		Document doc= (Document) answerList.stream()
							.filter(d -> d.get("_id").equals(new ObjectId(id)))
							.findAny()
							.orElse(null);
		if(id == null || id.equals("")) {
			return Response.status(404).entity("This answer does not exists!!").build();
		}else {
			collection.deleteOne(doc);
			return Response.status(202).entity("Answer deleted successfully !!").build();
		}
	}
}

