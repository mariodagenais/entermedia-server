package org.entermediadb.google;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.entermedia.util.EmTokenResponse;
import org.openedit.repository.ContentItem;
import org.openedit.util.OutputFiller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GoogleManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(GoogleManager.class);
	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;	
	protected OutputFiller filler = new OutputFiller();

	
	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}
	protected MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive)getModuleManager().getBean(getCatalogId(),"mediaArchive");
		}
		return fieldMediaArchive;
	}
	public Results listDriveFiles(Data authinfo, String inParentId) throws Exception
	{

		String url = "https://www.googleapis.com/drive/v3/files?fields=*";

//		String search = "mimeType = 'application/vnd.google-apps.folder'";
//		search = search + " and '" + inParentId + "' in parents";
		String search = "'" + inParentId + "' in parents";
		url = url + "&q=" + URLEncoder.encode(search);

		Results results = new Results();

		boolean keepgoing = false;
		do
		{
			keepgoing = populateMoreResults(authinfo, url, results);
		}
		while (keepgoing);

		return results;
	}

	protected boolean populateMoreResults(Data authinfo, String fileurl, Results results) throws Exception
	{
		if (results.getResultToken() != null)
		{
			fileurl = fileurl + "&pageToken=" + URLEncoder.encode(results.getResultToken(), "UTF-8");
		}
		JsonObject json = get(fileurl, authinfo);
		JsonElement pagekey = json.get("nextPageToken");
		if (pagekey != null)
		{
			results.setResultToken(pagekey.getAsString());
		}
		JsonArray files = json.getAsJsonArray("files");
		log.info(files);
		for (Iterator iterator = files.iterator(); iterator.hasNext();)
		{
			JsonObject object = (JsonObject) iterator.next();
			String name = object.get("name").getAsString();
			String id = object.get("id").getAsString();

			String mt = object.get("mimeType").getAsString();

			if (mt.equals("application/vnd.google-apps.folder"))
			{
				results.addFolder(object);
			}
			else
			{
				results.addFile(object);
			}
		}
		String keepgoing = json.get("incompleteSearch").getAsString();
		return Boolean.parseBoolean(keepgoing);
	}

	private JsonObject get(String inFileurl, Data authinfo) throws Exception
	{

		CloseableHttpClient httpclient;
		httpclient = HttpClients.createDefault();
		HttpRequestBase httpmethod = null;
		httpmethod = new HttpGet(inFileurl);
		String accesstoken = getAccessToken(authinfo);
		httpmethod.addHeader("authorization", "Bearer " + accesstoken);

		HttpResponse resp = httpclient.execute(httpmethod);

		if (resp.getStatusLine().getStatusCode() != 200)
		{
			log.info("Google Server error returned " + resp.getStatusLine().getStatusCode());
		}

		HttpEntity entity = resp.getEntity();
		String content = IOUtils.toString(entity.getContent());
		JsonParser parser = new JsonParser();
		JsonElement elem = parser.parse(content);
		//log.info(content);
		JsonObject json = elem.getAsJsonObject();
		if( json.get("error") != null) //Invalid Credentials
		{
			log.error("Could not connect API" + content);
			authinfo.setValue("httprequesttoken",null);
			getMediaArchive().getSearcher("oauthprovider").saveData(authinfo);
		}
		return json;

	}
	protected void saveFile(Data authinfo, Asset inAsset) throws Exception
	{
		CloseableHttpClient httpclient = HttpClients.createDefault();
		
//		GET https://www.googleapis.com/drive/v3/files/0B9jNhSvVjoIVM3dKcGRKRmVIOVU?alt=media
//		Authorization: Bearer <ACCESS_TOKEN>

		String url = "https://www.googleapis.com/drive/v3/files/" + inAsset.get("googleid") + "?alt=media";
		HttpRequestBase httpmethod = new HttpGet(url);
		String accesstoken = getAccessToken(authinfo);
		httpmethod.addHeader("authorization", "Bearer " + accesstoken);

		HttpResponse resp = httpclient.execute(httpmethod);

		if (resp.getStatusLine().getStatusCode() != 200)
		{
			log.info("Google Server error returned " + resp.getStatusLine().getStatusCode());
		}

		HttpEntity entity = resp.getEntity();
		
		ContentItem item = getMediaArchive().getOriginalContent(inAsset);
		
		File output = new File(item.getAbsolutePath());
		output.getParentFile().mkdirs();
		log.info("Google Manager Downloading " + item.getPath());
		filler.fill(entity.getContent(),new FileOutputStream(output),true);
		
		//getMediaArchive().getAssetImporter().reImportAsset(getMediaArchive(), inAsset);
		//ContentItem itemFile = getMediaArchive().getOriginalContent(inAsset);
		getMediaArchive().getAssetImporter().getAssetUtilities().getMetaDataReader().updateAsset(getMediaArchive(), item, inAsset);
		inAsset.setProperty("previewstatus", "converting");
		getMediaArchive().saveAsset(inAsset);
		getMediaArchive().fireMediaEvent( "assetimported", null, inAsset); //Run custom scripts?
		
//		if( assettype != null && assettype.equals("embedded") )
//		{
//			current.setValue("assettype","embedded");
//		}

		
	}	

	private String getAccessToken(Data authinfo) throws Exception
	{
		String accesstoken = authinfo.get("httprequesttoken"); //Expired in 14 days 
		if( accesstoken == null)
		{
		
			OAuthClientRequest request = OAuthClientRequest.tokenProvider(OAuthProviderType.GOOGLE).setGrantType(GrantType.REFRESH_TOKEN).setRefreshToken(authinfo.get("refreshtoken")).setClientId(authinfo.get("clientid")).setClientSecret(authinfo.get("clientsecret")).buildBodyMessage();
			OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
			//Facebook is not fully compatible with OAuth 2.0 draft 10, access token response is
			//application/x-www-form-urlencded, not json encoded so we use dedicated response class for that
			//Own response class is an easy way to deal with oauth providers that introduce modifications to
			//OAuth specification
			EmTokenResponse oAuthResponse = oAuthClient.accessToken(request, EmTokenResponse.class);
			// final OAuthAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request, "POST");
			// final OAuthAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request);
			accesstoken = oAuthResponse.getAccessToken();
			authinfo.setValue("httprequesttoken", accesstoken);
			getMediaArchive().getSearcher("oauthprovider").saveData(authinfo);
			
		}	
		return accesstoken;
	}

	public void syncAssets(Data inAuthinfo) 
	{
		try
		{
			Results results = listDriveFiles(inAuthinfo,"root");
			processResults(inAuthinfo,"Drive", results);
			getMediaArchive().fireSharedMediaEvent( "conversions/runconversions"); //this will save the asset as imported
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex);
		}
		
	}

	protected void processResults(Data inAuthinfo,  String inCategoryPath, Results inResults) throws Exception
	{
		createAssets(inAuthinfo, inCategoryPath,inResults.getFiles());
		
		if( inResults.getFolders() != null)
		{
			for (Iterator iterator = inResults.getFolders().iterator(); iterator.hasNext();)
			{
				JsonObject folder = (JsonObject) iterator.next();
				String id = folder.get("id").getAsString();
				String foldername = folder.get("name").getAsString();
				foldername = foldername.trim();
				Results folderresults = listDriveFiles(inAuthinfo,id);
				String categorypath = inCategoryPath +  "/" + foldername;
				processResults(inAuthinfo,categorypath,folderresults);
			}
		}	

	}

	protected void createAssets(Data authinfo, String categoryPath, Collection inFiles) throws Exception
	{
		if( inFiles == null)
		{
			return;
		}
		Category category = getMediaArchive().createCategoryPath(categoryPath);
		Collection tosave = new ArrayList();
		
		for (Iterator iterator = inFiles.iterator(); iterator.hasNext();)
		{
			JsonObject object = (JsonObject) iterator.next();
			String id = object.get("id").getAsString();
			//log.info(object.get("kind"));// "kind": "drive#file",
			//	String md5 = object.get("md5Checksum").getAsString();
			Data asset = (Asset) getMediaArchive().getAssetSearcher().query().exact("googleid", id).searchOne();
			if (asset == null)
			{
				Asset newasset = (Asset) getMediaArchive().getAssetSearcher().createNewData();
				String filename = object.get("name").getAsString();
				filename = filename.trim();
				JsonElement webcontentelem = object.get("webContentLink");

				newasset.setSourcePath(categoryPath + "/" + filename);
				newasset.setFolder(false);
				newasset.setValue("googleid", id);
				newasset.setName(filename);
//				if (webcontentelem != null)
//				{
//					newasset.setValue("importstatus", "needsdownload");
//					newasset.setValue("fetchurl", webcontentelem.getAsString());
//				}
				JsonElement jsonElement = object.get("webViewLink");
				if (jsonElement != null)
				{
					newasset.setValue("linkurl", jsonElement.getAsString());

				}
//				JsonElement thumbnailLink = object.get("thumbnailLink");
//				if (thumbnailLink != null)
//				{
//					newasset.setValue("fetchthumbnailurl", thumbnailLink.getAsString());
//				}

				
				//			newasset.setValue("md5hex", md5);
				newasset.addCategory(category);
				//inArchive.getAssetSearcher().saveData(newasset);
				tosave.add(newasset);
			} 
			else
			{
				//Update category
				
				//TODO: The clean up script should fix all these clearMissingOriginals
				
				Asset existing = (Asset)asset;
				//existing.clearCategories();
				if( !existing.isInCategory(category) )
				{
					//Clear old Drive categorties
					Category root = getMediaArchive().createCategoryPath("Drive");
					Collection existingcategories = new ArrayList( existing.getCategories());
					for (Iterator iterator2 = existingcategories.iterator(); iterator2.hasNext();)
					{
						Category drive = (Category ) iterator2.next();
						if( root.isAncestorOf(drive) )
						{
							existing.removeCategory(drive);
						}
					}
					existing.addCategory(category);
					getMediaArchive().saveAsset(existing);
				}	
			}
		}
		getMediaArchive().saveAssets(tosave);

		for (Iterator iterator = tosave.iterator(); iterator.hasNext();)
		{
			Asset asset = (Asset) iterator.next();
			saveFile(authinfo, asset);
			
		}
		
	}
	
}