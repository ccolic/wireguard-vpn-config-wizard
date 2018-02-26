/*
 * This software is GPL2 licensed, find further license information
 * in the LICENSE file located in the root directory
 *
 * Created on : 22-12-17
 * Authors    : Christian Colic, Marc Werenfels
 *
 */

package wgWizard.helper;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import java.io.File;

/**
 * Helper class to create a password protected ZIP file
 */
public class Zipper {
    private static final String EXTENSION = "zip";

    /**
     * Pack the given filePath in a ZIP and encrypt with the given password
     * @param filePath Path of the folder that you want to pack
     * @param password Password
     * @throws ZipException IO error
     */
    public static void pack(String filePath, String password) throws ZipException {
        String destinationZipFilePath = filePath + "/wireguard-configuration." + EXTENSION;

        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        zipParameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_ULTRA);
        zipParameters.setEncryptFiles(true);
        zipParameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
        zipParameters.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
        zipParameters.setPassword(password);

        ZipFile zipFile = new ZipFile(destinationZipFilePath);

        File inFolder = new File(filePath);
        File[] listOfFiles = inFolder.listFiles();

        assert listOfFiles != null;
        for (File file : listOfFiles) {
            if (file.isFile()) {
                zipFile.addFile(file, zipParameters);
            }
        }
    }
}
