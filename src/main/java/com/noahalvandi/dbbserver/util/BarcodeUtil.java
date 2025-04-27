package com.noahalvandi.dbbserver.util;

import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class BarcodeUtil {

    public static byte[] generateBarcodePng(String barcodeText) throws Exception {
        Barcode barcode = BarcodeFactory.createCode128(barcodeText);
        barcode.setBarWidth(2);
        barcode.setBarHeight(60);
        barcode.setDrawingText(true);

        BufferedImage image = BarcodeImageHandler.getImage(barcode);
        ByteArrayOutputStream barcodeOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", barcodeOutputStream);
        return barcodeOutputStream.toByteArray();
    }
}
