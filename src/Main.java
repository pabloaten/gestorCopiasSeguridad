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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/*
* @author Pablo Atenciano Jurado
*
* */
public class Main {

    private static final String SERVIDOR = "localhost";
    private static final int PUERTO = 21;
    private static final String USUARIO = "pablo";
    private static final String PASSWORD = "1234";
    private static final String CARPETA_SERVIDOR = "C:\\servidor";
    private static final String CARPETA_LOCAL = "C:\\server2";

    public static void main(String[] args) {
        // Programador de tareas para verificar cambios cada 5 segundos
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(Main::verificarCambios, 0, 50, TimeUnit.SECONDS);

        // Mantén el prograama en ejecución
        System.out.println("Presiona ENTER para detener el programa o ingresa la ruta de un archivo para subirlo:");
        Scanner scanner = new Scanner(System.in);
        String userInput;
        while (!(userInput = scanner.nextLine()).isEmpty()) {
            System.out.println(userInput);
            subirArchivo(new File("C:\\Users\\usuario\\Desktop\\"+userInput));
        }

        // Parar el programador de tareas
        scheduler.shutdown();
    }

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

            // Compara los archivos remotos con los locales
            for (FTPFile archivoRemoto : archivosRemotos) {
                String nombreArchivoRemoto = archivoRemoto.getName();
                boolean encontrado = false;
                for (File archivoLocal : archivosLocales) {
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

            // Compara los archivos locales con los remotos
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
                    subirArchivo(archivoLocal);
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

    private static void subirArchivo(File archivoLocal) {
        if (!archivoLocal.exists()) {
            System.out.println("El archivo especificado no existe.");
            return;
        }

        FTPClient ftpClient = new FTPClient();
        try {
            System.out.println("Subiendo archivo " + archivoLocal.getName() + " al servidor FTP...");

            // Conecta al servidor FTP
            ftpClient.connect(SERVIDOR, PUERTO);
            ftpClient.login(USUARIO, PASSWORD);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // Nombre del archivo comprimido con la fecha y hora actual
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String nombreArchivoComprimido = dateFormat.format(new Date()) + ".zip";

            // Comprimir archivo
            File archivoComprimido = comprimirArchivo(archivoLocal, nombreArchivoComprimido);

            // Sube el archivo comprimido al servidor FTP
            InputStream inputStream = new FileInputStream(archivoComprimido);
            boolean subido = ftpClient.storeFile(archivoComprimido.getName(), inputStream);
            inputStream.close();

            if (subido) {
                System.out.println("Se ha subido el archivo " + archivoComprimido.getName() + " al servidor FTP.");
            } else {
                System.out.println("Error al subir el archivo " + archivoComprimido.getName() + " al servidor FTP.");
            }

        } catch (IOException e) {
            System.out.println("Error al subir archivo: " + e.getMessage());
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

    private static File comprimirArchivo(File archivoLocal, String nombreArchivoComprimido) throws IOException {
        // Ruta del archivo comprimido
        String rutaArchivoComprimido = archivoLocal.getParent() + File.separator + nombreArchivoComprimido;

        // Stream de salida para el archivo comprimido
        FileOutputStream fos = new FileOutputStream(rutaArchivoComprimido);
        ZipOutputStream zipOut = new ZipOutputStream(fos);

        // Stream de entrada para el archivo original
        FileInputStream fis = new FileInputStream(archivoLocal);

        // Agrega el archivo original al archivo comprimido
        ZipEntry zipEntry = new ZipEntry(archivoLocal.getName());
        zipOut.putNextEntry(zipEntry);

        // Lee el archivo original y escribe en el archivo comprimido
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }

        // Cierra los streams
        fis.close();
        zipOut.close();
        fos.close();

        return new File(rutaArchivoComprimido);
    }
}
