En esta entrada voy a hablar de como implementar **filtros** en **Spring Boot**. Los filtros de los que voy a hablar son de los que se pueden establecer cuando se recibe una petición HTTP. Es decir, suponiendo que tenemos una programa que tiene una serie de funciones que se ejecutan al invocar unas URI, poder especificar, antes de que la petición sea procesada, que deseamos filtrar esas peticiones.

Esto es muy útil, por ejemplo si queremos que todas las peticiones cumplan un requisito, por ejemplo incluir una cabecera especifica.

Como siempre, como mejor se entiende un concepto es realizando un programa y explicandolo. El código fuente del programa que he realizado  para demostrar la utilidad de los filtros lo tenéis en [mi página de GITHUB](https://github.com/chuchip/SpringFilter)

En el programa de ejemplo tenemos un controlador para peticiones REST en la clase `PrincipalController.java` que será el encargado de gestionar todas las peticiones.

Este es parte del código de la clase.

```java
@RestController
public class PrincipalController {
	@Autowired
	SillyLog sillyLog;
	
	@GetMapping("*")
	public String entryOther(HttpServletRequest request,HttpServletResponse response)
	{	
		sillyLog.debug("In entryOther");
		if (response.getHeader("PROFE")!=null)
			sillyLog.debug("Header contains PROFE: "+response.getHeader("PROFE"));
		
		return "returning by function entryOther\r\n"+
				sillyLog.getMessage();
	}
	@GetMapping(value={"/","one"})
	public String entryOne(HttpServletRequest request,HttpServletResponse response	)
	{
		sillyLog.debug("In entryOne");
		if (response.getHeader("PROFE")!=null)
		{
			sillyLog.debug("Header contains PROFE: "+response.getHeader("PROFE"));
			return entryTwo(response);				
		}
		return "returning by function entryOne\r\n"+
				sillyLog.getMessage();
	}
	.....
```

En la función `entryOther` se capturaran todas las peticiones tipo **GET** que vayan a alguna URI que no tengamos definidas explícitamente. En la función `entryOne` se procesaran las peticiones tipo **GET**   que vayan a la URL http://localhost:8080/one o  http://localhost:8080/ 

En el fichero `MyFilter.java` definimos nuestro primer filtro.

```java
@Component
@Order(1)
public class MyFilter implements Filter{
	@Autowired
	SillyLog sillyLog;
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse  myResponse= (HttpServletResponse) response;
		sillyLog.debug("Filter: URL"
				+ " called: "+httpRequest.getRequestURL().toString());
		
		if (httpRequest.getRequestURL().toString().endsWith("/one"))	{			
			myResponse.addHeader("PROFE", "FILTERED");						
			chain.doFilter(httpRequest, myResponse);
			return;
		}
		if (httpRequest.getRequestURL().toString().endsWith("/redirect"))	{			
			myResponse.addHeader("PROFE", "REDIRECTED");
			myResponse.sendRedirect("redirected");
			chain.doFilter(httpRequest, myResponse);
			return;
		}
		if (httpRequest.getRequestURL().toString().endsWith("/cancel"))	{			
			myResponse.addHeader("PROFE", "CANCEL");
			myResponse.setStatus(HttpStatus.BAD_REQUEST.value());
			myResponse.getOutputStream().flush();
			myResponse.getOutputStream().println("-- Output by filter error --");
			chain.doFilter(httpRequest, myResponse);
			return;
		}
		chain.doFilter(request, response);
	}
}
```

Lo primero que tenemos que hacer para definir un filtro *general* es etiquetar la clase con **@Component** . Después deberemos implementar el interface `Filter` . También podríamos extender de la clase `OncePerRequestFilter` la cual implementa el interface `Filter` pero añade ciertas funcionalidades para que un filtro solo se ejecute una vez por ejecución. En este ejemplo vamos a simplificarlo al maximo y directamente implementaremos el interface `Filter`.

El interface `Filter` define tres funciones.

- `void init(FilterConfig filterConfig)    throws ServletException` 

  Esta función será ejecutada por el contenedor web. En otras palabras: esta función solo es ejecutada una vez, cuando el componente es instanciado por  **Spring**.

- `void doFilter(ServletRequest request,ServletResponse response,  FilterChain chain) throws IOException, ServletException`

  Esta función será ejecutada cada vez que se realiza una petición HTTP. En ella es donde podremos ver el contenido de la petición HTTP, en el objeto `ServletRequest` y modificar la respuesta en el objeto `ServletResponse` .  `FilterChain`  es lo que debemos ejecutar si queremos continuar la petición.

- `void destroy()`

  Esta función es llamada por el contenedor web de Spring para indicarle al filtro, que va a dejar de estar activo.

En el código del ejemplo nosotros solo utilizaremos la función `doFilter`




Hay maneras de especificar que un filtro solo sea activo para ciertas URL. En ese caso hay que registrarlo explícitamente y no seria necesario marcar la clase con la etiqueta **@Component**



Proyecto con ejemplos de filtros en Spring Boot.

Hacer llamada curl y que sigue los REDIRECT.

```
> curl -L -s http://localhost:8080/one
```

Muestra salida completa. Inlcuyendo codigo HTTP y HEADERS.

```
> curl -v -s http://localhost:8080/one
```