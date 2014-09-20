package com.steven.catanserver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public interface DataContainer<T extends DataContainer.Keyable> {
	
	T getElement(Integer id);
	
	public interface Keyable {
		int getId();
	}
	
	public class KeyedRelation<T extends Keyable> {
		
		private DataContainer<T> data;
		private transient ArrayList<T> cached;
		private transient Boolean cacheOK = false;
		HashSet<Integer> ids = new HashSet<Integer>();
		
		KeyedRelation(DataContainer<T> dc) {
			this.setup(dc);
		}
		
		DataContainer<T> getRawData() {
			return this.data;
		}
		
		void setup(DataContainer<T> data) {
			// have to deal with this class being auto-instantiated by GSON.
			this.data = data;
			this.cacheOK = false;
		}
		
		List<T> getAll() {
			if (this.cacheOK)
				return this.cached;
			ArrayList<T> ret = new ArrayList<T>(this.ids.size());
			for (Integer id : this.ids) {
				T el = data.getElement(id);
				if (el == null)
					System.out.println("Element with id " + id + " was not found in " + data.getClass().toString());
				ret.add(el);
			}
			cached = ret;
			cacheOK = true;
			return ret;
		}
		
		void add(T el) {
			this.ids.add(el.getId());
			cacheOK = false;
		}
	}
	
}
