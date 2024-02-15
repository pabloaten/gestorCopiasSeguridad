# Descripción del Proyecto

El proyecto consiste en una aplicación de sincronización bidireccional entre una carpeta local y un servidor FTP. Permite verificar los cambios entre los archivos presentes en la carpeta local y los archivos en el servidor FTP, y sincroniza los archivos faltantes entre ambos.

## Dependencias Utilizadas

El proyecto utiliza las siguientes dependencias de Apache Commons Net:

- `FTPClient`: Proporciona funcionalidades para interactuar con un servidor FTP.
- `FTPFile`: Representa un archivo en el servidor FTP.

## Funcionalidades Principales

### Verificación de Cambios

La aplicación programa una tarea para verificar cambios cada cierto intervalo de tiempo (en este caso, 50 segundos). La verificación de cambios involucra:

1. Conexión al servidor FTP.
2. Obtención de la lista de archivos en la carpeta remota del servidor y en la carpeta local.
3. Comparación de los archivos remotos con los locales y viceversa.
4. Descarga de archivos faltantes en la carpeta local.
5. Subida de archivos faltantes al servidor FTP.

### Subida de Archivos

La aplicación permite subir archivos de la carpeta local al servidor FTP. Para cada archivo a subir:

1. Comprime el archivo en formato ZIP con un nombre que incluye la fecha y hora actual.
2. Establece conexión con el servidor FTP.
3. Sube el archivo comprimido al servidor FTP.
4. Maneja errores y muestra mensajes en caso de éxito o fallo.

## Estructura del Código

- `Main.java`: Contiene el punto de entrada de la aplicación y las funciones principales para la verificación de cambios y la subida de archivos.
- `verificarCambios()`: Función que maneja la verificación de cambios.
- `descargarArchivo()`: Función para descargar archivos del servidor FTP a la carpeta local.
- `subirArchivo()`: Función para subir archivos de la carpeta local al servidor FTP.
- `comprimirArchivo()`: Función auxiliar para comprimir archivos en formato ZIP.

## Variables de Configuración

- `SERVIDOR`, `PUERTO`, `USUARIO`, `PASSWORD`: Información de conexión al servidor FTP.
- `CARPETA_SERVIDOR`: Ruta de la carpeta remota en el servidor FTP.
- `CARPETA_LOCAL`: Ruta de la carpeta local a sincronizar.

## Funcionamiento

1. La aplicación inicia un programador de tareas para verificar cambios.
2. Permite al usuario subir archivos al servidor FTP ingresando la ruta del archivo por la entrada estándar.
3. La verificación de cambios se ejecuta de forma periódica, comparando los archivos en la carpeta local y en el servidor FTP.
4. Se muestran mensajes indicando el estado de la sincronización y los cambios detectados.

## Conclusiones

El proyecto proporciona una solución simple pero efectiva para mantener sincronizados los archivos entre una carpeta local y un servidor FTP. Puede ser útil en escenarios donde se requiere mantener actualizada una carpeta local con los archivos presentes en un servidor FTP, y viceversa.
