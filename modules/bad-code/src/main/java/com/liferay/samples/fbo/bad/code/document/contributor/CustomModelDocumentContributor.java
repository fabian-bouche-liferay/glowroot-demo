package com.liferay.samples.fbo.bad.code.document.contributor;

import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.search.spi.model.index.contributor.ModelDocumentContributor;
import com.liferay.samples.fbo.bad.code.constants.GeoConstants;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata.GPSInfo;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
		immediate = true,
		property = "indexer.class.name=com.liferay.document.library.kernel.model.DLFileEntry",
		service = ModelDocumentContributor.class
	)
public class CustomModelDocumentContributor implements ModelDocumentContributor<DLFileEntry> {

	private final static Logger LOG = LoggerFactory.getLogger(CustomModelDocumentContributor.class);
	
	@Override
	public void contribute(Document document, DLFileEntry baseModel) {
		String fileName = baseModel.getFileName();
		if(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
			try {
				InputStream inputStream = baseModel.getContentStream();
				final JpegImageMetadata jpegMetadata = (JpegImageMetadata) Imaging.getMetadata(inputStream, fileName);
				GPSInfo gpsInfo = jpegMetadata.getExif().getGPS();
				if(gpsInfo != null) {
					double lat = gpsInfo.getLatitudeAsDegreesNorth();
					double lon = gpsInfo.getLongitudeAsDegreesEast();
					document.addGeoLocation(lat, lon);
					indexByLatAndLon(document, lat, lon);
				}
			} catch (PortalException e) {
				LOG.error("Failed to access image file", e);
			} catch (ImageReadException e) {
				LOG.error("Failed to read image metadata", e);
			} catch (IOException e) {
				LOG.error("Failed to read image", e);
			}			
		}
	}
	
	public void indexByLatAndLon(Document document, double lat, double lon) {
		CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpGet request = new HttpGet("https://nominatim.openstreetmap.org/reverse?lat=" + lat + "&lon=" + lon);
        CloseableHttpResponse response = null;
        
		try {
			response = httpClient.execute(request);
	        HttpEntity entity = response.getEntity();
	        if (entity != null) {
	            String result = EntityUtils.toString(entity);
	            String countryCode = SAXReaderUtil.read(result).getDocument().getRootElement().element("addressparts").element("country_code").getText();
				if(countryCode != null) {
					document.addText(GeoConstants.PHOTO_COUNTRY_CODE, countryCode);
				}
	            String city = SAXReaderUtil.read(result).getDocument().getRootElement().element("addressparts").element("city").getText();
				if(city != null) {
					document.addText(GeoConstants.PHOTO_CITY, city);
				}
				String ref = SAXReaderUtil.read(result).getDocument().getRootElement().element("result").attributeValue("ref");
				if(ref != null) {
					document.addText(GeoConstants.PHOTO_REFERENCE, ref);
				}
				
	        }

		} catch (ClientProtocolException e) {
			LOG.error("Client protocol exception", e);
		} catch (IOException e) {
			LOG.error("IO exception", e);
		} catch (DocumentException e) {
			LOG.error("Failed to parse XML", e);
		} finally {
	        if(response != null) {
	        	try {
					response.close();
				} catch (IOException e) {
					LOG.error("Failed to close response", e);
				}
	        }
	        try {
				httpClient.close();
			} catch (IOException e) {
				LOG.error("Failed to close http client", e);
			}
		}
		
	}

}
