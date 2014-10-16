package org.springfield.flanders.restlet;

import java.io.IOException;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.springfield.flanders.MPlayerMetaDataExtractor;
import org.springfield.flanders.MjpegIndexer;
import org.springfield.flanders.RtmpdumpMetadataExtractor;
import org.springfield.flanders.homer.LazyHomer;
import org.springfield.flanders.homer.MountProperties;
import org.springfield.flanders.tools.FileHelper;


public class FlandersResource extends Resource {

	private static Logger log = Logger.getLogger(FlandersResource.class);

	// the decimal format is used to parse the interval value of the request xml
	private static DecimalFormat df = new DecimalFormat("#.####");

	public FlandersResource(Context context, Request request, Response response) {
		super(context, request, response);

		// add representational variants allowed
		getVariants().add(new Variant(MediaType.TEXT_XML));
	}

	// allowed actions: POST, PUT, GET, DELETE
	public boolean allowPut() {
		return true;
	}

	public boolean allowPost() {
		return true;
	}

	public boolean allowGet() {
		return true;
	}

	/**
	 * GET get resource of type modelClass
	 */
	@Override
	public Representation getRepresentation(Variant variant) {
		Representation result = null;
		if (variant.getMediaType().equals(MediaType.TEXT_XML)) {
			result = new StringRepresentation("GET this...");
		}

		return result;
	}

	@Override
	public void post(Representation representation){
		String xml = "";
		log.error("entering the POST!!!!!");
		try {
			if (representation == null) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				getResponse().setEntity("<status>Error: the request data could not be read</status>",
						MediaType.TEXT_XML);
			} else {
				xml = representation.getText();
			}
		} catch (IOException e2) {
			e2.printStackTrace();
			return;
		}
		System.out.println("REPRESENTATION: " + xml);
		if (representation != null && xml != null) {
			Document document = null;
			try {
				document = DocumentHelper.parseText(xml);
			} catch (DocumentException e1) {
				e1.printStackTrace();
			}
			Node node = document.selectSingleNode("//root");
			String mount = node.valueOf("mount");
			String source = node.valueOf("source");
			String stream = node.valueOf("stream");
			String filename = node.valueOf("file");
			String mjpegIndex = node.valueOf("index");
			
			if (mjpegIndex!=null && mount!=null && source!=null) {
				MountProperties mp = LazyHomer.getMountProperties(mount);
				if (mp != null) {
					source = mp.getPath() + source;					
					String response = MjpegIndexer.extractMetaData(source, mount);
					getResponse().setEntity(new StringRepresentation(response));
				} else {
					getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
					getResponse().setEntity("<status>Error: internal mount properties not found</status>", MediaType.TEXT_XML);
				}
			} else if(source!=null && mount!=null) {
				MountProperties mp = LazyHomer.getMountProperties(mount);
				source = mp.getPath() + source;				
				String ext = FileHelper.getFileExtension(source);
				if(ext != null){
					String response = MPlayerMetaDataExtractor.extractMetaData(source);
					getResponse().setEntity(new StringRepresentation(response));
				} else {
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
					getResponse().setEntity("<status>Error: incorrect parameters</status>", MediaType.TEXT_XML);
				}
			} else if(stream!=null && filename!=null) {
				String response = RtmpdumpMetadataExtractor.extractMetaData(stream,filename);
				getResponse().setEntity(new StringRepresentation(response));
			} else {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				getResponse().setEntity("<status>Error: incorrect parameters</status>", MediaType.TEXT_XML);
			}
		}
	}

}