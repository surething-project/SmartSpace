# Streaming
This document shall give a small overview of the new streaming functionality added to the VSL.  
Two new methods have been added to the `VslConnector` interface:  
```java
/**
 * This method is used to stream data from a virtual node at an address.
 * Callers of this method must close the returned {@link InputStream}.
 *
 * @param address
 *      the node address in the VSL.
 * @return
 *      the {@link InputStream} handle where data can be read from.
 * @throws VslException
 *      If a VSL exception occurs.
 */
InputStream getStream(String address) throws VslException;

/**
 * This method is used to stream data to a virtual node at an address.
 * Closes the provided {@link InputStream}.
 *
 * @param address
 *      the node address in the VSL.
 * @param stream
 *      the {@link InputStream} containing the data that should be sent.
 * @throws VslException
 *      If a VSL exception occurs.
 */
void setStream(String address, InputStream stream) throws VslException;
```
Both of these methods only work with **virtual nodes**.  
`getStream()` opens a stream on a virtual node and returns a stream handle (`InputStream`) 
that can be used to access the data. The caller must take care of closing the stream.  
`setStream()` takes as argument a stream handle and reads the data off of it in a new `Thread`, so the method
does not block. The method takes care to close the provided stream, if an error occurs.

### Minimal Example
In this example, we assume a **Service1**, which wants to first retrieve data via a stream from **Service2**,
and then send it back to it, also using a stream.

##### Service 2
**Service2** must register a virtual node for this, so assume this minimal model file:
```xml
<service2 type="/basic/composed">
    <test-stream type="/basic/text" />
</service2>
```
Then, it can register a virtual node and define handlers for both get and set stream.  
```java
connector.registerVirtualNode("/vsl/address/to/service2/test-stream", new VirtualNodeAdapter() {
    @Override
    public InputStream getStream(String address, VslIdentity identity) throws VslException {
        return new ByteArrayInputStream("test".getBytes());
    }
    @Override
    public void setStream(String address, InputStream stream, VslIdentity identity) throws VslException {
        final byte[] buf = new byte[64];
        final int n;
        try {
            n = stream.read(buf);
        } catch (IOException e) {
            throw new UnexpectedErrorException(e.getMessage());
        }
        System.out.println(Arrays.toString(Arrays.copyOfRange(buf, 0, n)));
    }
});
```

##### Service 1
**Service1** now can execute its request using the new streaming methods of the `ServiceConnector`.
```java
// Get the stream from Service2.
String data = "";
try (final InputStream stream = connector.getStream("/vsl/address/to/service2/test-stream")) {
    final byte[] buf = new byte[64];
    final int n = stream.read(buf)
    data = Arrays.toString(Arrays.copyOfRange(buf, 0, n));
} catch (Exception e) {
    throw new UnexpectedErrorException(e.getMessage());
}

// Send the data back to it.
connector.setStream("/vsl/address/to/service2/test-stream", new ByteArrayInputStream(data.getBytes()));
```

### Code changes
#### Changes per module
- core: 
  - added get and setStream to the interfaces (`VslConnector`, ...)
  - added stream utility funcs to utils
  - added some exceptions
- jetty-http2:
  - refactored HTTP2 code and added Conscrypt JSP (Java 7 & 8 ALPN & general performance)
- rest-connector:
  - added streaming functionality
  - added Conscrypt JSP
  - added handler to WebSocket, to send the streams requested by another service to the KA
  - JettyClient:
    - added proper lock Object to access websocketClient
    - replaced anonymous VSLExceptions with proper Exception classes.
- rest-transport:
  - added streaming functionality
    - usage of Futures to connect streams between WebSocket-Thread and HTTP-Handler-Thread
    - added a serial to the WebSocket Callback to distinguish between multiple awaited streams for the same virtual node
  - added Conscypt JSP
- ka-wiring:
  - implemented streaming methods in Request Router, very similar to other methods
  - same for cache request router
- kor:
  - implemented streaming methods in KOR
  - added `checkRootWriteAccess` to `VslNodeTree` interface, to check write access to virtual node for setStream
- java7-/java8-connector:
  - implemented new streaming methods  

In general, cleanup of Maven dependencies, most of all regarding the import of rest-connector and rest-transport. Also introduced one jetty version variable in the `parent.pom` file, so that Jetty can be upgraded uniformly across all modules. Bumped targetJDK from 1.7 to 1.8

#### Known issues
- The current Jetty version is the last that works, after that an exception in the SSL certificate appears.
- Sometimes, streams may take a bit longer on a KA to be processed/closed. This is due to the constant flushing of the stream data in the `core.utils.Stream.copy()` methods.
  The flushing, however, is needed, since Jetty buffers the HTTP responses before actually sending them. If the streams would not be flushed, small data that is sent over the streams
  is not delivered immediately and remain trapped in the buffer. 
  It is not a solution to make the response buffers of Jetty smaller/disable them, since then the performance drastically suffers. I would use WebSockets instead of HTTP streaming,
  once WebSockets are supported in Jetty for HTTP/2
