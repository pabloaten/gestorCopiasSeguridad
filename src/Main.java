import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Clase principal que gestiona la transferencia de archivos entre una carpeta local y un servidor FTP.
 */
public class Main {

    private static final String SERVIDOR = "localhost";
    private static final int PUERTO = 21;
    private static final String USUARIO = "pablo";
    private static final String PASSWORD = "1234";
    private static final String CARPETA_SERVIDOR = "C:\\servidor";
    private static final String CARPETA_LOCAL = "C:\\server2";

    /**
     * Método principal que inicia el programa.
     * @param args Argumentos de la línea de comandos (no utilizado).
     */
    public static void main(String[] args) {
        FTPClient ftpClient = new FTPClient();
        // Programador de tareas para verificar cambios cada 10 segundos
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(Main::verificarCambios, 0, 10, TimeUnit.SECONDS);
        try {
            ftpClient.connect(SERVIDOR, PUERTO);
            ftpClient.login(USUARIO, PASSWORD);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Mantén el programa en ejecución
        System.out.println("Presiona ENTER para detener el programa o ingresa la ruta de un archivo para subirlo:");
        Scanner scanner = new Scanner(System.in);
        String userInput;
        while (!(userInput = scanner.nextLine()).isEmpty()) {
            System.out.println(userInput);
            subirArchivo(ftpClient, new File("C:\\Users\\usuario\\Desktop\\" + userInput));
        }

        // Parar el programador de tareas
        scheduler.shutdown();
    }

    /**
     * Método que verifica cambios entre la carpeta local y el servidor FTP.
     */
    private static void verificarCambios() {
        FTPClient ftpClient = new FTPClient();
        try {
            System.out.println("Verificando cambios...");

            // Conecta al servidor FTP
            ftpClient.connect(SERVIDOR, PUERTO);
            ftpClient.login(USUARIO, PASSWORD);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // Obtiene la lista de archivos en la carpeta remota del servidor FTP
            FTPFile[] archivosRemotos = ftpClient.listFiles();

            // Obtiene la lista de archivos en la carpeta local
            File carpetaLocal = new File(CARPETA_LOCAL);
            File[] archivosLocales = carpetaLocal.listFiles();

            // Verifica si algún archivo remoto ha sido borrado
            for (FTPFile archivoRemoto : archivosRemotos) {
                String nombreArchivoRemoto = archivoRemoto.getName();
                boolean encontrado = false;
                for (File archivoLocal : archivosLocales) {
                    // Compara solo si los nombres de los archivos coinciden
                    if (archivoLocal.getName().equals(nombreArchivoRemoto)) {
                        encontrado = true;
                        break;
                    }
                }
                if (!encontrado) {
                    // Descargar el archivo remoto que falta en la carpeta local
                    descargarArchivo(ftpClient, nombreArchivoRemoto, new File(CARPETA_LOCAL, nombreArchivoRemoto));
                }
            }

            // Verifica si algún archivo local debe ser subido al servidor
            for (File archivoLocal : archivosLocales) {
                String nombreArchivoLocal = archivoLocal.getName();
                boolean encontrado = false;
                for (FTPFile archivoRemoto : archivosRemotos) {
                    if (archivoRemoto.getName().equals(nombreArchivoLocal)) {
                        encontrado = true;
                        break;
                    }
                }
                if (!encontrado) {
                    // Subir el archivo local que falta en el servidor FTP
                    subirArchivo(ftpClient, archivoLocal, nombreArchivoLocal);
                }
            }

        } catch (IOException e) {
            System.out.println("Error al verificar cambios: " + e.getMessage());
        } finally {
            try {
                // Cierra la conexión FTP
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Método que descarga un archivo del servidor FTP.
     * @param ftpClient Cliente FTP
     * @param nombreRemoto Nombre del archivo en el servidor FTP
     * @param archivoLocal Archivo local donde se descargará el archivo
     * @throws IOException Si hay un error de E/S durante la descarga
     */
    private static void descargarArchivo(FTPClient ftpClient, String nombreRemoto, File archivoLocal) throws IOException {
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(archivoLocal));
        boolean descargado = ftpClient.retrieveFile(nombreRemoto, outputStream);
        outputStream.close();

        if (descargado) {
            System.out.println("Se ha descargado el archivo " + nombreRemoto + " desde el servidor FTP.");
        } else {
            System.out.println("Error al descargar el archivo " + nombreRemoto + " desde el servidor FTP.");
        }
    }

    /**
     * Método que sube un archivo al servidor FTP.
     * @param ftpClient Cliente FTP
     * @param archivoLocal Archivo local que se subirá al servidor FTP
     * @param nombreArchivoRemoto Nombre del archivo en el servidor FTP
     */
    private static void subirArchivo(FTPClient ftpClient, File archivoLocal, String nombreArchivoRemoto) {
        if (!archivoLocal.exists()) {
            System.out.println("El archivo especificado no existe.");
            return;
        }

        try {
            System.out.println("Subiendo archivo " + archivoLocal.getName() + " con nombre " + nombreArchivoRemoto + " al servidor FTP...");

            // Comprimir archivo
            File archivoComprimido = comprimirArchivo(archivoLocal, nombreArchivoRemoto);

            // Subir el archivo al servidor FTP
            InputStream inputStream = new FileInputStream(archivoComprimido);
            boolean subido = ftpClient.storeFile(nombreArchivoRemoto, inputStream);
            inputStream.close();

            if (subido) {
                System.out.println("Se ha subido el archivo " + archivoLocal.getName() + " con nombre " + nombreArchivoRemoto + " al servidor FTP.");
            } else {
                System.out.println("Error al subir el archivo " + archivoLocal.getName() + " con nombre " + nombreArchivoRemoto + " al servidor FTP.");
            }

        } catch (IOException e) {
            System.out.println("Error al subir archivo: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Método que sube un archivo al servidor FTP con un nombre basado en la fecha actual.
     * @param ftpClient Cliente FTP
     * @param archivoLocal Archivo local que se subirá al servidor FTP
     */
    private static void subirArchivo(FTPClient ftpClient, File archivoLocal) {
        if (!archivoLocal.exists()) {
            System.out.println("El archivo especificado no existe.");
            return;
        }

        try {
            // Obtener la fecha actual
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-hhmmss");
            Date fechaActual = new Date();
            String nombreArchivoRemoto = sdf.format(fechaActual) + ".zip"; // Nombre del archivo con la fecha actual y extensión .zip

            System.out.println("Subiendo archivo " + archivoLocal.getName() + " con nombre " + nombreArchivoRemoto + " al servidor FTP...");

            // Comprimir archivo
            File archivoComprimido = comprimirArchivo(archivoLocal, nombreArchivoRemoto);

            // Subir el archivo al servidor FTP
            InputStream inputStream = new FileInputStream(archivoComprimido);
            boolean subido = ftpClient.storeFile(nombreArchivoRemoto, inputStream);
            inputStream.close();

            if (subido) {
                System.out.println("Se ha subido el archivo " + archivoLocal.getName() + " con nombre " + nombreArchivoRemoto + " al servidor FTP.");
            } else {
                System.out.println("Error al subir el archivo " + archivoLocal.getName() + " con nombre " + nombreArchivoRemoto + " al servidor FTP.");
            }

        } catch (IOException e) {
            System.out.println("Error al subir archivo: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Método que comprime un archivo utilizando 7zip.
     * @param archivoLocal Archivo local que se comprimirá
     * @param nombreArchivoComprimido Nombre del archivo comprimido
     * @return El archivo comprimido
     * @throws IOException Si hay un error de E/S durante la compresión
     * @throws InterruptedException Si el hilo actual es interrumpido mientras espera a que finalice el proceso
     */
    private static File comprimirArchivo(File archivoLocal, String nombreArchivoComprimido) throws IOException, InterruptedException {
        // Ruta del archivo comprimido
        String rutaArchivoComprimido = archivoLocal.getParent() + File.separator + nombreArchivoComprimido;

        // Comando para comprimir usando 7zip desde el CMD
        String comando = "\"C:\\Program Files\\7-Zip\\7z.exe\" a \"" + rutaArchivoComprimido + "\" \"" + archivoLocal.getAbsolutePath() + "\"";

        // Ejecutar el comando usando el CMD
        Process proceso = Runtime.getRuntime().exec(comando);
        proceso.waitFor();

        // Verificar si la compresión fue exitosa
        if (proceso.exitValue() == 0) {
            return new File(rutaArchivoComprimido);
        } else {
            throw new IOException("Error al comprimir el archivo usando 7zip");
        }
    }
}
