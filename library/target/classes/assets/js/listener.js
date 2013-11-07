$(document).ready(function() {  
        if(window.WebSocket) {
        var client;
        var destination;
        var socket="ws://54.215.210.214:61623";
        var apolloLogin = "admin";
        var apolloPassword = "password"
        var port = location.port;

        if (port==8001)
                {var destination = "/topic/05452.book.all";}
        else 
                {var destination = "/topic/05452.book.computer";}        
              
        client = Stomp.client(socket, "stomp");
        client.connect(apolloLogin, apolloPassword, function() {
            client.debug("connected to Stomp");
            //alert("connected");
            client.subscribe(destination);
          });
        }
});  