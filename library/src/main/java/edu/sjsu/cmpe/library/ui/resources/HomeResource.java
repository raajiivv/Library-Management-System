package edu.sjsu.cmpe.library.ui.resources;

//import java.util.List;

import javax.jms.JMSException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.yammer.dropwizard.jersey.params.LongParam;

import edu.sjsu.cmpe.library.domain.Book;
import edu.sjsu.cmpe.library.domain.Book.Status;
import edu.sjsu.cmpe.library.repository.BookRepositoryInterface;
import edu.sjsu.cmpe.library.ui.views.HomeView;

@Path("/")
@Produces(MediaType.TEXT_HTML)
public class HomeResource {
    private final BookRepositoryInterface bookRepository;

    public HomeResource(BookRepositoryInterface bookRepository) {
	this.bookRepository = bookRepository;
    }

@GET
public HomeView getHome() {
	//List<Book> tempBooks = bookRepository.getAllBooks();
	//tempBooks = tempBooks.
    return new HomeView(bookRepository.getAllBooks());
	}

@PUT
@Path("/{isbn}")

public void updateBookStatus(@PathParam("isbn") LongParam isbn,
    @DefaultValue("available") @QueryParam("status") Status status) throws JMSException {
Book book = bookRepository.getBookByISBN(isbn.get());
if (book.getStatus().equals(Status.available)) {
	book = bookRepository.updateBookStatus(book, status);//book.setStatus(status);
}
/*BookDto bookResponse = new BookDto(book);
String location = "/books/" + book.getIsbn();
bookResponse.addLink(new LinkDto("view-book", location, "GET"));

return Response.status(200).entity(bookResponse).build();*/
}
}
