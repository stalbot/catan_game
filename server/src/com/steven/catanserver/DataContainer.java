package com.steven.catanserver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public interface DataContainer<T extends DataContainer.Keyable> {
	
	T getElement(Integer id);
	
	void finalizeFromDB(BoardModel board);
	
	public interface Keyable {
		int getId();
	}
	
	public class KeyedRelation<T extends Keyable> {
		
		private transient DataContainer<T> data = null;
		private transient ArrayList<T> cached;
		private transient Boolean cacheOK = false;
		ArrayList<Integer> ids = new ArrayList<Integer>();
 		private transient HashSet<Integer> idsSet = new HashSet<Integer>();
		
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
			this.idsSet = new HashSet<Integer>(ids);
		}
		
		List<T> getAll() {
			if (this.cacheOK)
				return this.cached;
			ArrayList<T> ret = new ArrayList<T>(this.ids.size());
			for (Integer id : this.ids) {
				T el = this.data.getElement(id);
				if (el == null)
					System.out.println("Element with id " + id + " was not found in " + data.getClass().toString());
				ret.add(el);
			}
			cached = ret;
			cacheOK = true;
			return ret;
		}
		
		void add(T el) {
			if (!this.idsSet.add(el.getId()))
				return;
			this.ids.add(el.getId());
			cacheOK = false;
		}
		
		void set(int index, T el) {
			// TODO: allow this to move an existing element around?
			if (!this.idsSet.add(el.getId()))
				return;
			while (this.ids.size() < index + 1)
				this.ids.add(null);
			this.ids.set(index, el.getId());
			cacheOK = false;
		}
	}
	
}
