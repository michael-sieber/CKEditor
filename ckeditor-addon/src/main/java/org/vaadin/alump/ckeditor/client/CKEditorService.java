/**
 * CKEditorService.java (CKEditor)
 *
 * Copyright 2017 Vaadin Ltd, Sami Viitanen <sami.viitanen@vaadin.org>
 *
 * Based on CKEditor from Yozons, Inc, Copyright (C) 2010-2016 Yozons, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vaadin.alump.ckeditor.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ScriptElement;

/**
 * GWT wrapper for CKEDITOR for use by our Vaadin-based CKEditorService.
 */
public class CKEditorService {
	
	private static boolean libraryLoadInited = false;
	private static boolean libraryLoaded = false;
	private static List<ScheduledCommand> afterLoadedStack = new ArrayList<ScheduledCommand>();
	
	public static synchronized void loadLibrary(ScheduledCommand afterLoad) {
		if (! libraryLoadInited) {
			libraryLoadInited = true;
			if(!libraryReady()) {
				String url = GWT.getModuleBaseURL() + "ckeditor/ckeditor.js";
				ScriptElement se = Document.get().createScriptElement();
				se.setSrc(url);
				se.setType("text/javascript");
				Document.get().getElementsByTagName("head").getItem(0).appendChild(se);
			}
			Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {				
				@Override
				public boolean execute() {
					if (libraryReady()) {
						reduceBlurDelay();
						for (ScheduledCommand sc: afterLoadedStack) {
							sc.execute();
						}
						libraryLoaded = true;
						return false;
					}
					return true;
				}
			}, 50);
		}
		if (libraryLoaded) {
			afterLoad.execute();
		} else {
			afterLoadedStack.add(afterLoad);
		}
	}
	
	public static native boolean libraryReady()
	/*-{
		if($wnd.CKEDITOR) {
			return true;
		} 
		return false;
	}-*/;
	
	/**
	 * Use this method to load editor to given identifier.
	 * 
	 * @param id the string DOM <div> 'id' attribute value for the element you want to replace with CKEditor
	 * @param listener the CKEditorService.CKEditorListener will get notified when the editor instance is ready, changed, etc.
	 * @param jsInPageConfig the String possible custom "in page" configuration; note that this must be an expected JSON for the CKEDITOR in page config. sent "as is" without any real syntax or security testing, so be sure you know it's valid and not malicious,such as: <code>{toolbar : 'Basic', language : 'en'}</code>
	 */
	public static native JavaScriptObject loadEditor(String id, CKEditorService.CKEditorListener listener, String jsInPageConfig, int compWidth, int compHeight,
																									 String startupMode)
	/*-{
	 	// Build our inPageConfig object based on the JSON jsInPageConfig sent to us.
	 	var inPageConfig = @org.vaadin.alump.ckeditor.client.CKEditorService::convertJavaScriptStringToObject(Ljava/lang/String;)(jsInPageConfig);
	 	
	 	var myEditor;
	 	
	 	if (inPageConfig == null) {
	 		inPageConfig = new Object;
	 		inPageConfig.width = compWidth;
	 		inPageConfig.height = compHeight;
	 	} else {
	 		if (!inPageConfig.width) inPageConfig.width = compWidth;
	 		if (!inPageConfig.height) inPageConfig.height = compHeight;
		}

		inPageConfig.startupMode = startupMode;

		myEditor = $wnd.CKEDITOR.appendTo( id, inPageConfig );
	 		 	
	 	// The 'listener' passed to us is used as 'listenerData' for the callback.
		myEditor.on( 'instanceReady', function( ev ) {
    		ev.listenerData.@org.vaadin.alump.ckeditor.client.CKEditorService.CKEditorListener::onInstanceReady()();
		}, null, listener);
		
		return myEditor;

	}-*/;
	
	public native static String version()
	/*-{
		return $wnd.CKEDITOR.version;
	}-*/;
	
	// This is a hack attempt to resolve issues with Vaadin when the CKEditorTextField widget is set with BLUR and FOCUS listeners.
	// In particular, the Safari browser could not deal well with PASTE, right clicking in a table cell, etc.
	// because those operations resulted in BLUR then FOCUS events in rapid succession, causing the UI to update.
	// But the 200 value is too long and we find that often the button acts faster than the BLUR can fire from CKEditor
	// so Vaadin doesn't get the latest contents.
	// Even though CKEditor 4.2 introduced a change event, it doesn't appear to fire if you stay in SOURCE mode, which many people do use.
	public native static void reduceBlurDelay()
	/*-{
		$wnd.CKEDITOR.focusManager._.blurDelay = 20; // the default is 200 even if the documentation says it's only 100
	}-*/;
	
	/**
	 * Returns a javascript CKEDITOR.editor instance for given id.
	 * 
	 * @param id the String id of the editor instance
	 * @return the overlay for CKEDITOR.editor or null if not yet initialized
	 */
	public native static CKEditor get(String id)
	/*-{
		return $wnd.CKEDITOR.instances[ id ];
	}-*/;
	
	// TODO: Never tested yet
	public native static void addStylesSet(String name, String jsStyles)
	/*-{
	 	var styles = @org.vaadin.alump.ckeditor.client.CKEditorService::convertJavaScriptStringToObject(Ljava/lang/String;)(jsStyles);
		$wnd.CKEDITOR.addStylesSet(name,styles);
	}-*/;
	
	// TODO: Never tested yet
	public native static void addTemplates(String name, String jsDefinition)
	/*-{
	 	var definition = @org.vaadin.alump.ckeditor.client.CKEditorService::convertJavaScriptStringToObject(Ljava/lang/String;)(jsDefinition);
		$wnd.CKEDITOR.addTemplates(name,definition);
	}-*/;

	public native static JavaScriptObject convertJavaScriptStringToObject(String jsString)
	/*-{
	    try {
	 		return eval('('+jsString+')');
	 	} catch (e) { 
	 		alert('convertJavaScriptStringToObject() INVALID JAVASCRIPT: ' + jsString); 
	 		return {}; 
	 	}
	}-*/;


	/**
	 * An interface for the VCKEditorTextField to get events from the CKEditor.
	 */
	public interface CKEditorListener {
		void onInstanceReady();
		void onChange();
		void onSelectionChange();
		void onModeChange(String mode);
		void onDataReady();
		void onBlur();
		void onFocus();
		void onSave();
		void onResize(Number[] pData);
	}

}
