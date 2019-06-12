En esta entrada voy a hablar de como implementar **filtros** en **Spring Boot**. Los filtros de los que voy a hablar son de los que se pueden establecer cuando se recibe una petición HTTP. Es decir, suponiendo que tenemos una programa que tiene una serie de funciones que se ejecutan al invocar unas URI, poder especificar, antes de que la petición sea procesada, que deseamos filtrar esas peticiones.

Esto es muy útil, por ejemplo si queremos que todas las peticiones cumplan un requisito, por ejemplo incluir una cabecera especifica.

Como siempre, como mejor se entiende un concepto es realizando un programa y explicandolo. El código fuente del programa que he realizado  para demostrar la utilidad de los filtros lo tenéis en [mi página de GITHUB](https://github.com/chuchip/SpringFilter)

En el programa de ejemplo tenemos un controlador para peticiones REST en la clase `PrincipalController.java` que será el encargado de gestionar todas las peticiones.

Este es el código de la clase:

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
	@GetMapping("two")
	public String entryTwo(HttpServletResponse response)
	{
		sillyLog.debug("In entryTwo");
		if (response.getHeader("PROFE")!=null)
			sillyLog.debug("Header contains PROFE: "+response.getHeader("PROFE"));
		return "returning by function entryTwo\r\n"+
				sillyLog.getMessage();
	}
	@GetMapping("three")
	public String entryThree()
	{
		sillyLog.debug("In entryThree");
		return "returning by function entryThree\n"+
				sillyLog.getMessage();
	}
	@GetMapping("redirected")
	public String entryRedirect(HttpServletRequest request)
	{
		sillyLog.debug("In redirected");
		return "returning by function entryRedirect\n"+
				sillyLog.getMessage();
	}
}
```

En la función `entryOther` se capturaran todas las peticiones tipo **GET** que vayan a alguna URI que no tengamos definidas explícitamente. En la función `entryOne` se procesaran las peticiones tipo **GET**   que vayan a la URL http://localhost:8080/one o  http://localhost:8080/ 

La clase `sillyLog` es una clase donde simplemente iremos añadiendo líneas de log para luego devolverlas en la petición, de tal manera que podremos ver por donde ha pasado nuestra petición.

En esta aplicación se definen dos filtros: `MyFilter.java` y `OtherFilter.java`. El primero tiene preferencia sobre el segundo, por estar así establecido en el parámetro de la etiqueta **@Order**.

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
        if (httpRequest.getRequestURL().toString().endsWith("/none"))	{   
            myResponse.setStatus(HttpStatus.BAD_GATEWAY.value());
		    myResponse.getOutputStream().flush();
		    myResponse.getOutputStream().println("-- I don't have any to tell you --");
            return; // No hago nada.
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

La clase `OtherFilter` es más simple:

```java
@Component
@Order(2)
public class OtherFilter implements Filter{
	@Autowired
	SillyLog sillyLog;
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest= (HttpServletRequest) request;
		HttpServletResponse  myResponse= (HttpServletResponse) response;
		sillyLog.debug("OtherFilter: URL"
				+ " called: "+httpRequest.getRequestURL().toString());
		if (myResponse.getHeader("PROFE")!=null)
		{
			sillyLog.debug("OtherFilter: Header contains PROFE: "+myResponse.getHeader("PROFE"));
		}
		chain.doFilter(request, response);
	}

}
```

Lo primero que tenemos que hacer para definir un filtro *general* es etiquetar la clase con **@Component** . Después deberemos implementar el interface `Filter` . También podríamos extender de la clase `OncePerRequestFilter` la cual implementa el interface `Filter` pero añade ciertas funcionalidades para que un filtro solo se ejecute una vez por ejecución. En este ejemplo vamos a simplificarlo al máximo y directamente implementaremos el interface `Filter`.

El interface `Filter` tiene tres funciones.

- `void init(FilterConfig filterConfig) throws ServletException` 

  Esta función será ejecutada por el contenedor web. En otras palabras: esta función solo es ejecutada una vez, cuando el componente es instanciado por  **Spring**.

- `void doFilter(ServletRequest request,ServletResponse response,  FilterChain chain) throws IOException, ServletException`

  Esta función será ejecutada cada vez que se realiza una petición HTTP. En ella es donde podremos ver el contenido de la petición HTTP, en el objeto `ServletRequest` y modificar la respuesta en el objeto `ServletResponse` .  `FilterChain`  es lo que debemos ejecutar si queremos continuar la petición.

- `void destroy()`

  Esta función es llamada por el contenedor web de Spring para indicarle al filtro, que va a dejar de estar activo.

Como he comentado anteriormente, la etiqueta **@Order** nos permitirá especificar el orden en que los filtros serán ejecutados. En este caso, este filtro tendrá el valor 1 y el siguiente tendrá el valor 2, por lo cual `MyFilter` se ejecutara antes que `OtherFilter`

La clase `MyFilter` realiza diferentes acciones según la URL llamada. La clase `OtherFilter `solamente añade un log cuando pasa por ella.

En el código del ejemplo  nosotros solo utilizamos la función `doFilter`. En ella, lo primero, convertimos la clase `ServletResponse` a  `HttpServletResponse` y la clase `ServletRequest` a `HttpServletRequest`. Esto es necesario para poder acceder a ciertas propiedades de los objetos que de otra manera no estarían disponibles. 

Voy a explicar paso  a paso los diferentes casos contemplados en la clase `MyFilter`, dependiendo de la URL invocada.

- **/one**:  Añadimos una cabecera **PROFE** con el valor **FILTERED**. Después ejecutamos la función `doFilter`de la clase `chain` con lo cual se continuara el flujo de la petición. Es decir, se ejecutaría el segundo filtro y después llegaría a la función `entryOne` del controlador, donde podríamos ver que existe un *header* con el valor PROFE, por lo cual se llama a la función entryTwo.

  Una llamada a esta URL nos devolvería lo siguiente:

  ```
  > curl  -s http://localhost:8080/one
  returning by function entryTwo
  SillyLog: 15eb34c2-cfac-4a27-9450-b3b07f44cb50/1 Filter: URL called: http://localhost:8080/one
  SillyLog: 15eb34c2-cfac-4a27-9450-b3b07f44cb50/2 OtherFilter: URL called: http://localhost:8080/one
  SillyLog: 15eb34c2-cfac-4a27-9450-b3b07f44cb50/3 OtherFilter: Header contains PROFE: FILTERED
  SillyLog: 15eb34c2-cfac-4a27-9450-b3b07f44cb50/4 In entryOne
  SillyLog: 15eb34c2-cfac-4a27-9450-b3b07f44cb50/5 Header contains PROFE: FILTERED
  SillyLog: 15eb34c2-cfac-4a27-9450-b3b07f44cb50/6 In entryTwo
  SillyLog: 15eb34c2-cfac-4a27-9450-b3b07f44cb50/7 Header contains PROFE: FILTERED
  
  ```

  La primera línea es devuelta por la función `entryTwo`. A continuación se muestran los logs añadidos.

  Creo que si se mira el código se ve claro el flujo.

- **/redirect**  Añadimos una cabecera **PROFE** con el valor **REDIRECTED**. Después especificamos que se debe incluir una redireción a la URL `redirected` con la instrucción `sendRedirect`. Después ejecutamos la función `doFilter` por lo cual se procesara el segundo filtro y se llamara a la función `entryOther` ya que no tenemos ningún punto de entrada definido para */cancel*.

  

- **/none** . Establezco el código HTTP  a devolver a BAD_GATEWAY y en el cuerpo pongo el texto *"I don't have any to tell you"*. No ejecuto la función `doFilter` por lo cual ni será llamado el segundo filtro, ni seria invocada ninguna llamada del Controlador en caso de que existiera alguno.

  ```curl
  > curl  -s http://localhost:8080/none
  -- I don't have any to tell you --
  ```

- **/cancel** . Establezco el código HTTP  a devolver a BAD_REQUEST y en el cuerpo pongo el texto *"Output by filter error"*.  Ejecuto la función `doFilter` por lo cual será ejecutado el filtro `OtherFilter` y se pasara por la función `entryOther` ya que no tenemos ningún punto de entrada definido para */cancel*

  ```curl
  > curl  -s http://localhost:8080/cancel
  -- Output by filter error --
  returning by function entryOther
  SillyLog: 1cf7f7f9-1a9b-46a0-9b97-b8d5caf734bd/1 Filter: URL called: http://localhost:8080/cancel
  SillyLog: 1cf7f7f9-1a9b-46a0-9b97-b8d5caf734bd/2 OtherFilter: URL called: http://localhost:8080/cancel
  SillyLog: 1cf7f7f9-1a9b-46a0-9b97-b8d5caf734bd/3 OtherFilter: Header contains PROFE: CANCEL
  SillyLog: 1cf7f7f9-1a9b-46a0-9b97-b8d5caf734bd/4 In entryOther
  SillyLog: 1cf7f7f9-1a9b-46a0-9b97-b8d5caf734bd/5 Header contains PROFE: CANCEL
  
  ```

  Observar que el cuerpo añadido en el filtro es anterior a lo devuelto por el controlador.

- 








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