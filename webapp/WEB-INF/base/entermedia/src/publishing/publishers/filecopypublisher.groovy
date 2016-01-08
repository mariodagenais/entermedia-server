package publishing.publishers;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.publishing.BasePublisher
import org.entermediadb.asset.publishing.PublishResult
import org.entermediadb.asset.publishing.Publisher
import org.openedit.Data
import org.openedit.entermedia.publishing.*
import org.openedit.page.Page
import org.openedit.util.FileUtils

public class filecopypublisher extends BasePublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(filecopypublisher.class);
	
	public PublishResult publish(MediaArchive mediaArchive,Asset inAsset, Data inPublishRequest, Data inDestination, Data inPreset)
	{
		
		PublishResult result = checkOnConversion(mediaArchive,inPublishRequest,inAsset,inPreset); 
		if( result != null)
		{
			return result;
		}

		result = new PublishResult();

		//Now publish it!		
		Page inputpage = findInputPage(mediaArchive,inAsset,inPreset);
		String destinationpath = inDestination.get("url");
		if(!destinationpath.endsWith("/"))
		{
			destinationpath = destinationpath + "/";
		}
		String exportname = inPublishRequest.get("exportname");
		//String guid = inPreset.get("guid");
		
		FileUtils utils = new FileUtils();
		File destination = new File(destinationpath);
		File source = new File(inputpage.getContentItem().getAbsolutePath());
		File finalfile = new File(destination, exportname);
		utils.copyFiles(source, finalfile);
		result.setComplete(true);
		
		log.info("published ${finalfile}");
		return result;
	}
	
}