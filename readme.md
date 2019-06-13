En esta entrada voy a hablar de como implementar **filtros** en **Spring**. Los filtros son de los que se pueden establecer cuando se recibe una petición HTTP. Es decir, suponiendo que tenemos un programa escuchando en unas  URI, poder especificar que deseamos ejecutar algo antes de que las peticiones sea procesadas por el controlador.

Esto es muy útil si queremos que todas las peticiones cumplan un requisito, por ejemplo incluir una cabecera especifica.

Para entender como funcionan los filtros en **Spring** he realizado un programa que iré explicando poco a poco. 

El código fuente del programa lo tenéis en [mi página de GITHUB](https://github.com/chuchip/SpringFilter)

Empezare mostrando el controlador para peticiones REST que esta en la clase `PrincipalController.java`. Este será el encargado de gestionar todas las peticiones.

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
		if (response.getHeader("CAKE")!=null)
			sillyLog.debug("Header contains CAKE: "+response.getHeader("CAKE"));
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

En la función `entryOther` se capturaran todas las peticiones tipo **GET** que vayan a alguna URI que no tengamos definidas explícitamente. En la función `entryOne` se procesaran las peticiones tipo **GET**   que vayan a la URL http://localhost:8080/one o  http://localhost:8080/  y así sucesivamente.

La clase `sillyLog` es una clase donde simplemente iremos añadiendo líneas de log para luego devolverlas en el *body* de la respuesta, de tal manera que podremos ver por donde ha pasado nuestra petición.

En esta aplicación se definen tres filtros: `MyFilter.java` ,`OtherFilter.java` y `CakesFilter.java`.  El primero tiene preferencia sobre el segundo, por estar así establecido en el parámetro de la etiqueta **@Order**. Del tercero  hablo al final del articulo.

En el fichero `MyFilter.java` definimos nuestro primer filtro.

```java
@Component
@Order(1)
public class MyFilter implements Filter{
	@Autowired
	SillyLog sillyLog;
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)	throws IOException, ServletException {
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

Lo primero que tenemos que hacer para definir un filtro *general* es etiquetar la clase con **@Component** . Después deberemos implementar el interface `Filter` . También podríamos extender de la clase `OncePerRequestFilter` la cual implementa el interface `Filter` y añade ciertas funcionalidades para que un filtro solo se ejecute una vez por ejecución. En este ejemplo vamos a simplificarlo al máximo y directamente implementaremos el interface `Filter`.

El interface `Filter` tiene tres funciones.

- `void init(FilterConfig filterConfig) throws ServletException` 

  Esta función será ejecutada por el contenedor web. En otras palabras: esta función solo es ejecutada una vez, cuando el componente es instanciado por  **Spring**.

- `void doFilter(ServletRequest request,ServletResponse response,  FilterChain chain) throws IOException, ServletException`

  Esta función será ejecutada cada vez que se realiza una petición HTTP. En ella es donde podremos ver el contenido de la petición HTTP, en el objeto `ServletRequest` y modificar la respuesta en el objeto `ServletResponse` .  `FilterChain`  es lo que debemos ejecutar si queremos continuar la petición.

- `void destroy()`

  Esta función es llamada por el contenedor web de Spring para indicarle al filtro, que va a dejar de estar activo.

Como he comentado anteriormente, la etiqueta **@Order** nos permitirá especificar el orden en que los filtros serán ejecutados. En este caso, este filtro tendrá el valor 1 y el siguiente tendrá el valor 2, por lo cual `MyFilter` se ejecutara antes que `OtherFilter`.

La clase `MyFilter` realiza diferentes acciones según la URL llamada. La clase `OtherFilter `solamente añade un log cuando pasa por ella.

En el código del ejemplo  nosotros solo utilizamos la función `doFilter`. En ella, lo primero, convertimos la clase `ServletResponse` a  `HttpServletResponse` y la clase `ServletRequest` a `HttpServletRequest`. Esto es necesario para poder acceder a ciertas propiedades de los objetos que de otra manera no estarían disponibles. 

Voy a explicar paso  a paso los diferentes casos contemplados en la clase `MyFilter`, dependiendo de la URL invocada.

- **/one**:  Añadimos una cabecera **PROFE** con el valor **FILTERED** a la respuesta. Es importante recalcar que solo podremos modificar la respuesta, la petición es inalterable.

   Después ejecutamos la función `doFilter`de la clase `chain` con lo cual se continuara el flujo de la petición. En este caso se ejecutaría el segundo filtro y después se pasaría por a la función `entryOne` del controlador, donde podríamos ver que existe un *header* con el valor **PROFE**, por lo cual se llama a la función `entryTwo`.

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

  Lo mejor es mirar el código fuente si no se tiene claro de donde salen tantas líneas ;-)

- **/redirect**  Añadimos una cabecera **PROFE** con el valor **REDIRECTED** al *response*. Después especificamos que se debe incluir una redirección a la URL `redirected` con la instrucción `myResponse.sendRedirect`. Finalmente ejecutamos la función `doFilter` por lo cual se procesara el segundo filtro y se llamara a la función `entryOther` ya que no tenemos ningún punto de entrada definido para */cancel*.

  Esta es la salida que tendremos si realizamos una petición con **curl:**

  ```curl
  > curl -s http://localhost:8080/redirect
  
  ```

  Efectivamente, no hay salida. ¿Por qué?. Pues porque hemos incluido una directiva *redirected* y **curl** por defecto no sigue esas directivas, con lo cual simplemente no muestra nada.

  Veamos, que esta pasando añadiéndole a **curl** el parámetro -v (verbose)

  ```curl
  curl -v -s http://localhost:8080/redirect
  *   Trying ::1...
  * TCP_NODELAY set
  * Connected to localhost (::1) port 8080 (#0)
  > GET /redirect HTTP/1.1
  > Host: localhost:8080
  > User-Agent: curl/7.60.0
  > Accept: */*
  >
  < HTTP/1.1 302
  < PROFE: REDIRECTED
  < Location: http://localhost:8080/redirected
  < Content-Length: 0
  < Date: Thu, 13 Jun 2019 13:57:44 GMT
  <
  * Connection #0 to host localhost left intact
  ```

  Esto es otra cosa, ¿verdad?. Ahora muestra en la cabecera nuestro valor para **PROFE** Y vemos la orden de redirigir a http://localhost:8080/redirected. Observar que el código HTTP es 302, que es *redirect*.

  Sí le decimos a **curl** que siga  la redirección, pasándole el parámetro **-L**, veremos lo que esperábamos.

  ```
  > curl -L -s http://localhost:8080/redirect
  returning by function entryRedirect
  SillyLog: dcfc8b09-84a4-40a1-a2d6-43340abdf50c/1 Filter: URL called: http://localhost:8080/redirected
  SillyLog: dcfc8b09-84a4-40a1-a2d6-43340abdf50c/2 OtherFilter: URL called: http://localhost:8080/redirected
  SillyLog: dcfc8b09-84a4-40a1-a2d6-43340abdf50c/3 In redirected
  
  ```

  Bueno, casi lo que esperábamos. Obsérvese que ha habido dos  peticiones HTTP a nuestro servicio  y solo se muestra los datos de la segunda. 

- **/none** . Establezco el código HTTP  a devolver a BAD_GATEWAY y en el cuerpo pongo el texto *"I don't have any to tell you"*. No ejecuto la función `doFilter` por lo cual ni será llamado el segundo filtro, ni seria pasada al controlador.

  ```curl
  > curl  -s http://localhost:8080/none
  -- I don't have any to tell you --
  ```

- **/cancel** . Establezco el código HTTP  a devolver a BAD_REQUEST y en el cuerpo pongo el texto *"Output by filter error"*.  Ejecuto la función `doFilter` por lo cual será ejecutado el filtro `OtherFilter` y se pasara por la función `entryOther` del controlador, ya que no tenemos ningún punto de entrada definido para */cancel*

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

- **Otros** En cualquier otra llamada se invocara la función `doFilter`de la clase `chain` por lo cual se pasara al siguiente filtro y después a la función del controlador adecuado.

  ```curl
  > curl -L -s http://localhost:8080/three
  returning by function entryThree
  SillyLog: a2dd979f-4779-4e34-b8f6-cae814370426/1 Filter: URL called: http://localhost:8080/three
  SillyLog: a2dd979f-4779-4e34-b8f6-cae814370426/2 OtherFilter: URL called: http://localhost:8080/three
  SillyLog: a2dd979f-4779-4e34-b8f6-cae814370426/3 In entryThree
  ```

Para especificar que un filtro solo sea activo para ciertas URL, hay que registrarlo explícitamente y no marcar la clase con la etiqueta **@Component**. En el proyecto de ejemplo en la clase `FiltrosApplication` vemos la función donde se añade un filtro:

```
@Bean
	public FilterRegistrationBean<CakesFilter> cakesFilter()
	{
		FilterRegistrationBean<CakesFilter> registrationBean = new FilterRegistrationBean<>();				
		registrationBean.setFilter(new CakesFilter());
		registrationBean.addUrlPatterns("/cakes/*");
		return registrationBean;
	}
```

La clase `CakesFilter` es la siguiente:

```
@Order(3)
public class CakesFilter implements Filter{		
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {		
		HttpServletResponse  myResponse= (HttpServletResponse) response;
		myResponse.addHeader("CAKE", "EATEN");	
		chain.doFilter(request, response);
	}
}
```

Al hacer una llamada a una url que empiece por */cakes/\** veremos como se ejecuta el ultimo filtro. 

```
 curl  -s http://localhost:8080/cakes
returning by function entryOther
SillyLog: 41e2c9b9-f8d2-42cc-a017-08ea6089e646/1 Filter: URL called: http://localhost:8080/cakes
SillyLog: 41e2c9b9-f8d2-42cc-a017-08ea6089e646/2 OtherFilter: URL called: http://localhost:8080/cakes
SillyLog: 41e2c9b9-f8d2-42cc-a017-08ea6089e646/3 In entryOther
SillyLog: 41e2c9b9-f8d2-42cc-a017-08ea6089e646/4 Header contains CAKE: EATEN

```

Por la manera en que tiene **Spring** de gestionar sus variables de contexto, no es posible inyectar el objeto `SillyLog` con un **@Autowired** . Si lo inyectamos veremos como la variable tiene el valor **null**

Y con esto doy por finalizada esta entrada ¡¡ Hasta la próxima !!