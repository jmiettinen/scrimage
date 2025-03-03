package com.sksamuel.scrimage.webp;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

abstract class WebpHandler {

   private static final Logger logger = LoggerFactory.getLogger(WebpHandler.class);

   protected static Path getPathFromProperty(String name) {
      try {
         String binaryDir = System.getProperty("com.sksamuel.scrimage.webp.binary.dir");
         if (binaryDir != null && !binaryDir.isEmpty()) {
            Path path = Paths.get(binaryDir, name);
            if (Files.isExecutable(path)) {
               return path;
            }
         }
         return null;
      } catch (Exception ignored) {
         return null;
      }
   }

   protected static Path createPlaceholder(String name) throws IOException {
      return Files.createTempFile(name, "binary");
   }

   protected static void installBinary(Path output, String... sources) throws IOException {
      logger.info("Installing binary at " + output);
      for (String source : sources) {
         logger.debug("Trying source from " + source);
         InputStream in = WebpHandler.class.getResourceAsStream(source);
         if (in != null) {
            logger.debug("Source detected " + source);
            Files.copy(in, output, StandardCopyOption.REPLACE_EXISTING);
            in.close();

            if (!SystemUtils.IS_OS_WINDOWS) {
               logger.info("Setting executable " + output);
               setExecutable(output);
            }
            return;
         }
      }
      throw new IOException("Could not locate webp binary at " + Arrays.toString(sources));
   }

   /**
    * Returns the search paths to locate the webp binaries for the given binary.
    */
   protected static String[] getBinaryPaths(String binaryName) {

      String osArch = System.getProperty("os.arch");
      if (osArch == null) osArch = "";

      if (SystemUtils.IS_OS_WINDOWS) {
         return windows(binaryName);
      } else if ("mac_arm64".equals(System.getProperty("com.sksamuel.scrimage.webp.platform"))) {
         return macArm(binaryName);
      } else if (SystemUtils.IS_OS_MAC && (osArch.startsWith("arm") || osArch.startsWith("aarch64"))) {
         return macArm(binaryName);
      } else if (SystemUtils.IS_OS_MAC) {
         return macIntel(binaryName);
      } else {
         return linux(binaryName);
      }
   }

   private static String[] macIntel(String binaryName) {
      return new String[]{
         "/webp_binaries/" + binaryName,
         "/webp_binaries/mac/" + binaryName,
         "/dist_webp_binaries/libwebp-1.3.2-mac-x86-64/bin/" + binaryName,
      };
   }

   private static String[] macArm(String binaryName) {
      return new String[]{
         "/webp_binaries/" + binaryName,
         "/webp_binaries/mac_arm64/" + binaryName,
         "/dist_webp_binaries/libwebp-1.3.2-mac-arm64/bin/" + binaryName,
      };
   }

   private static String[] windows(String binaryName) {
      return new String[]{
         "/webp_binaries/" + binaryName,
         "/webp_binaries/" + binaryName + ".exe",
         // typo from previous versions must be left in
         "/webp_binaries/window/" + binaryName,
         "/webp_binaries/window/" + binaryName + ".exe",
         "/webp_binaries/windows/" + binaryName,
         "/webp_binaries/windows/" + binaryName + ".exe",
         "/dist_webp_binaries/libwebp-1.3.2-windows-x64/bin/" + binaryName,
         "/dist_webp_binaries/libwebp-1.3.2-windows-x64/bin/" + binaryName + ".exe",
      };
   }

   private static String[] linux(String binaryName) {
      return new String[]{
         "/webp_binaries/" + binaryName,
         "/webp_binaries/linux/" + binaryName,
         "/dist_webp_binaries/libwebp-1.3.2-linux-x86-64/bin/" + binaryName,
      };
   }

   private static void setExecutable(Path output) throws IOException {
      try {
         new ProcessBuilder("chmod", "+x", output.toAbsolutePath().toString())
            .start()
            .waitFor(30, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
         throw new IOException(e);
      }
   }
}
