$(":button").click(function() {
	var isbn = this.id;
	alert('About to report lost on ISBN ' + isbn);
	Callback(isbn);
	$("#"+isbn).attr("disabled", "disabled");
	
});

function Callback(isbn)
{
	//alert(isbn);
	$.ajax({
	    type: "PUT",
	    url: "http://localhost:8001/library/v1/books/"+isbn,
	    contentType: "application/json",
	    data: {"status": "lost"}
	});
	$(status).text("lost")
}