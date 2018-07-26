package org.entermediadb.asset.sources;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.importer.PathChangedListener;
import org.entermediadb.asset.util.TimeParser;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.locks.Lock;
import org.openedit.repository.ContentItem;
import org.openedit.util.DateStorageUtil;

public class AssetSourceManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(AssetSourceManager.class);
	
	protected MediaArchive fieldMediaArchive;
	protected Collection fieldAssetSources;
	protected AssetSource fieldDefaultAssetSource;
	protected String fieldCatalogId;
	protected ModuleManager fieldModuleManager;
	
	
	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Bob#getOriginalDocumentStream(org.entermediadb.asset.Asset)
	 */
	
	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public AssetSource getDefaultAssetSource()
	{
		return fieldDefaultAssetSource;
	}

	public void setDefaultAssetSource(AssetSource inDefaultAssetSource)
	{
		fieldDefaultAssetSource = inDefaultAssetSource;
	}

	public Collection getAssetSources()
	{
		if( fieldAssetSources == null)
		{
			fieldAssetSources = new ArrayList();
			//Search hot folders and load by type
			Collection hits = getMediaArchive().query("hotfolder").all().sort("orderingDown").sort("lastscanstart").search();
			for (Iterator iterator = hits.iterator(); iterator.hasNext();)
			{
				Data config = (Data) iterator.next();
				loadSource(config);
			}
		}
		return fieldAssetSources;
	}

	public void setAssetSources(Collection inAssetSources)
	{
		fieldAssetSources = inAssetSources;
	}

	public InputStream getOriginalDocumentStream(Asset inAsset) throws OpenEditException
	{
		InputStream page = findAssetSource(inAsset).getOriginalDocumentStream(inAsset);
		return page;
	}

	public ContentItem getOriginalContent(Asset inAsset)
	{
		ContentItem page = findAssetSource(inAsset).getOriginalContent(inAsset);
		return page;

	}
	
	protected AssetSource findAssetSource(Asset inAsset)
	{
		for (Iterator iterator = getAssetSources().iterator(); iterator.hasNext();)
		{
			AssetSource source = (AssetSource) iterator.next();
			if( source.handles(inAsset))
			{
				return source;
			}
		}
		return getDefaultAssetSource();
	}

	public MediaArchive getMediaArchive()
	{
		if( fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive)getModuleManager().getBean(getCatalogId(),"mediaArchive");
		}
		return fieldMediaArchive;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Bob#setMediaArchive(org.entermediadb.asset.MediaArchive)
	 */
	
	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	public boolean removeOriginal(Asset inAsset)
	{
		boolean ok = findAssetSource(inAsset).removeOriginal(inAsset);
		return ok;
	}

	public void addNewAsset(Asset inAsset, List<ContentItem> inTemppages)
	{
		findAssetSource(inAsset).addNewAsset(inAsset,inTemppages);
	}

	public void replaceOriginal(Asset inAsset, List<ContentItem> inTemppages)
	{
		findAssetSource(inAsset).replaceOriginal(inAsset,inTemppages);
	}

	public void assetAdded(Asset inAsset, ContentItem inContentItem)
	{
		// TODO Auto-generated method stub
		findAssetSource(inAsset).assetAdded(inAsset,inContentItem);
	}


	public Searcher getFolderSearcher(String inCatalogId)
	{
		return getMediaArchive().getSearcherManager().getSearcher(inCatalogId, "hotfolder");
	}
/*
	public Data getFolderByPathEnding(String inCatalogId, String inFolder)
	{		
		for (Iterator iterator = loadFolders(inCatalogId).iterator(); iterator.hasNext();)
		{
			Data folder = (Data) iterator.next();
			String subfolder = folder.get("subfolder");
			if(inFolder.equals(subfolder) )
			{
				return folder;
			}
			
		}
		return null;
	}
*/

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.scanner.HotFolderManager2#deleteFolder(java.lang.String, org.openedit.Data)
	public void deleteFolder(String inCatalogId, Data inExisting)
	{
		//String type = inExisting.get("hotfoldertype");
		getFolderSearcher(inCatalogId).delete(inExisting, null);
//		if( "syncthing".equals(type))
//		{
//			updateSyncThingFolders(inCatalogId);
//		}
		
	}
	*/

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.scanner.HotFolderManager2#saveFolder(java.lang.String, org.openedit.Data)
	 */
	public void saveSourceConfig(Data inNewrow)
	{
		removeSource(inNewrow);
		AssetSource source = loadSource(inNewrow);
		source.saveConfig();
		/*
		String type = inNewrow.get("hotfoldertype");
		if("syncthing".equals(type))
		{
			String toplevelfolder = inNewrow.get("subfolder");
			Page toplevel = getPageManager().getPage("/WEB-INF/data/" + inCatalogId + "/hotfolders/" + toplevelfolder +"/" );
			if(!toplevel.exists()){
				getPageManager().putPage(toplevel);
			}
			inNewrow.setProperty("externalpath",toplevel.getContentItem().getAbsolutePath() );
			getFolderSearcher(inCatalogId).saveData(inNewrow, null);
			//updateSyncThingFolders(inCatalogId);
		}
		else if( "googledrive".equals(type))
		{
			String toplevelfolder = inNewrow.get("subfolder");
			String email = inNewrow.get("email");
			MediaArchive archive = (MediaArchive)getSearcherManager().getModuleManager().getBean(inCatalogId,"mediaArchive");

			ContentItem hotfolderpath =  archive.getContent( "/WEB-INF/data/" + archive.getCatalogId() + "/workingfolders/"+ email + "/" + toplevelfolder );
			File file = new File( hotfolderpath.getAbsolutePath() );
			file.mkdirs();
			inNewrow.setProperty("externalpath",file.getAbsolutePath());				
			getFolderSearcher(inCatalogId).saveData(inNewrow, null);
			archive.fireMediaEvent("hotfolder", "googledrivesaved", inNewrow.getId(), null);
			
		}
		else if( "resiliodrive".equals(type))
		{
			String toplevelfolder = inNewrow.get("subfolder");
			MediaArchive archive = (MediaArchive)getSearcherManager().getModuleManager().getBean(inCatalogId,"mediaArchive");
			
			ContentItem hotfolderpath =  archive.getContent( "/WEB-INF/data/" + archive.getCatalogId() + "/workingfolders/"+ toplevelfolder );
			File file = new File( hotfolderpath.getAbsolutePath() );
			file.mkdirs();
			inNewrow.setProperty("externalpath",file.getAbsolutePath());				
			getFolderSearcher(inCatalogId).saveData(inNewrow, null);
			archive.fireMediaEvent("hotfolder","resiliodrivesaved", inNewrow.getId(), null);
		}
		else 
		{
		
			getFolderSearcher(inCatalogId).saveData(inNewrow, null);
		}	
		*/
	}	
	
	public void checkForDeleted()
	{
		Collection sources = getAssetSources();
		for(Iterator iterator = sources.iterator(); iterator.hasNext();)
		{
			AssetSource source = (AssetSource)iterator.next();
			source.checkForDeleted();
		}
	}
	public AssetSource loadSource(Data config)
	{
		AssetSource source = getSourceById(config.getId());
		if( source == null)
		{
			String type = config.get("hotfoldertype");
			if( type == null)
			{
				type = "mount";
			}
			source = (AssetSource)getModuleManager().getBean(getCatalogId(),type + "AssetSource");
			source.setConfig(config);
			source.setMediaArchive(getMediaArchive());
			fieldAssetSources.add(source);
		}
		//Sort?
		return source;
	}

	public List<String> importHotFolder(AssetSource inSource, String basepath)
	{
		return inSource.importAssets(basepath);
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.scanner.HotFolderManager2#importHotFolder(org.entermediadb.asset.MediaArchive, org.openedit.Data)
	 */
	public void scanSources(ScriptLogger inLog)
	{
		Collection sources = getAssetSources();
		for(Iterator iterator = sources.iterator(); iterator.hasNext();)
		{
			AssetSource source = (AssetSource)iterator.next();
			Data data = source.getConfig();
			if( skipCheck(data))
			{
				inLog.info("Source already checked");
				continue;
			}
//			String base = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals";
			String name = data.get("subfolder");
//			String path = base + "/" + name ;
			Lock lock = getMediaArchive().getLockManager().lockIfPossible("scan-" + data.getId(), "HotFolderManager");
			if( lock == null)
			{
				inLog.info("folder is already in lock table" + name);
				continue;
			}
			try
			{
				Object enabled = data.getValue("enabled");
				if( enabled != null && "false".equals( enabled.toString() ) )
				{
					inLog.info("Hot folder not enabled " + name);
					continue;
				}
				inLog.info("Starting hot folder import " + name);
	
				try
				{
					//pullGit(path,1);
					Collection found = source.importAssets(null); 
					if( found != null)
					{
						inLog.info(name + " Imported " + found.size() + " assets");
					}
				}
				catch( Exception ex)
				{
					log.error("Could not process folder ${path}",ex);
				}
				
			}
			finally
			{
				try
				{
					getMediaArchive().releaseLock(lock);
				}
				catch ( Exception ex)
				{
					//We somehow got a version error. Someone save it from under us
					//TOOD: Delete them all?
				}
			}
		}
	}

	protected boolean skipCheck(Data inFolder)
	{
		long sincedate = 0;
		String since = inFolder.get("lastscanstart");
		if( since != null )
		{
			sincedate = DateStorageUtil.getStorageUtil().parseFromStorage(since).getTime();
		}
		boolean skipmodcheck = false;
		if( since != null )
		{
			long now = System.currentTimeMillis();
			String mod = getMediaArchive().getCatalogSettingValue("importing_modification_interval");
			if( mod == null)
			{
				mod = "1d";
			}
			long time = new TimeParser().parse(mod);
			long expires = sincedate + time; //once a week
			if( now < expires ) //our last time + 24 hours
			{
				skipmodcheck = true;
			}
		}
		return skipmodcheck;
	}

	public void removeSource(Data inData)
	{
		AssetSource source = getSourceById(inData.getId());
		source.detach();
		getAssetSources().remove(source);
		getMediaArchive().getSearcher( "hotfolder").delete(inData, null);
	}
	

	public AssetSource getSourceById(String inFolderId)
	{
		for (Iterator iterator = getAssetSources().iterator(); iterator.hasNext();)
		{
			AssetSource source = (AssetSource) iterator.next();
			if( source.getConfig().getId().equals(inFolderId))
			{
				return source;
			}
			
		}
		return null;
	}

}