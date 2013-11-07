$(":button").click(function() {
	var isbn = this.id;
	alert('About to report lost on ISBN ' + isbn);
	Callback(isbn);
	$("#"+isbn).attr("disabled", "disabled");
	loaction.reload(true);
});

function Callback(isbn)
{
	//alert(isbn);
	$.ajax({
	    type: "PUT",
	    url: "/library/v1/books/"+isbn+"/?status=lost",
	    contentType: "application/json",
	    success: function(data) {
            window.location.reload();
    }	
	});
	$(status).text("lost")
	
	
}
