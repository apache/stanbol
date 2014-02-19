/**
 * Copyright 2007 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.paoding.analysis.dictionary.support.detection;

import java.io.File;
import java.io.FileFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 * @since 2.0.2
 * 
 */
public class Detector implements Runnable {

	private Logger log = LoggerFactory.getLogger(Detector.class);

	private DifferenceListener listener;

	private File home;

	private FileFilter filter;

	private long interval;

	private Snapshot lastSnapshot;
	
	private Thread thread;

	private boolean alive = true;

	public void setListener(DifferenceListener listener) {
		this.listener = listener;
	}

	public Detector() {
	}

	/**
	 * 检查间隔
	 * 
	 * @param interval
	 */
	public void setInterval(int interval) {
		this.interval = interval * 1000;
	}

	public void setHome(File home) {
		this.home = home;
	}

	public void setHome(String home) {
		this.home = new File(home);
	}

	public void setFilter(FileFilter filter) {
		this.filter = filter;
	}
	
	public Snapshot flash(){
		return Snapshot.flash(home, filter);
	}

	public void start(boolean daemon) {
		if (lastSnapshot == null) {
			lastSnapshot = flash();
		}
		thread = new Thread(this);
		thread.setDaemon(daemon);
		thread.start();
	}
	
	
	public Snapshot getLastSnapshot() {
		return lastSnapshot;
	}
	
	public void setLastSnapshot(Snapshot last) {
		this.lastSnapshot = last;
	}

	public void run() {
		if (interval <= 0)
			throw new IllegalArgumentException(
					"should set a interval(>0) for the detection.");
		while (alive) {
			sleep();
			forceDetecting();
		}
	}

	public void forceDetecting() {
		Snapshot current = flash();
		Difference diff = current.diff(lastSnapshot);
		if (!diff.isEmpty()) {
			try {
				listener.on(diff);
				log.info("found differen for " + home);
				log.info("{}",diff);
				lastSnapshot = current;
			} catch (Exception e) {
				log.error("", e);
			}
		}
	}

	public void setStop() {
		alive = false;
		thread = null;
	}

	private void sleep() {
		try {
			Thread.sleep(interval);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Detector d = new Detector();
		d.setInterval(1);
		d.setHome(new File("dic"));
		d.setFilter(new ExtensionFileFilter(".dic"));
		d.setListener(new DifferenceListener() {
			public void on(Difference diff) {
				System.out.println(diff);
			}

		});
		d.start(false);
	}

}
