package com.firefly.codec.http2.hpack;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.codec.http2.model.HttpScheme;
import com.firefly.codec.http2.model.StaticTableHttpField;
import com.firefly.utils.StringUtils;
import com.firefly.utils.collection.ArrayQueue;
import com.firefly.utils.collection.ArrayTernaryTrie;
import com.firefly.utils.collection.Trie;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

/**
 * HPACK - Header Compression for HTTP/2
 * <p>
 * This class maintains the compression context for a single HTTP/2 connection.
 * Specifically it holds the static and dynamic Header Field Tables and the
 * associated sizes and limits.
 * </p>
 * <p>
 * It is compliant with draft 11 of the specification
 * </p>
 */
public class HpackContext {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private static final String EMPTY = "";
	
	public static final String[][] STATIC_TABLE =
	    {
	        {null,null},
	        /* 1  */ {":authority",EMPTY},
	        /* 2  */ {":method","GET"},
	        /* 3  */ {":method","POST"},
	        /* 4  */ {":path","/"},
	        /* 5  */ {":path","/index.html"},
	        /* 6  */ {":scheme","http"},
	        /* 7  */ {":scheme","https"},
	        /* 8  */ {":status","200"},
	        /* 9  */ {":status","204"},
	        /* 10 */ {":status","206"},
	        /* 11 */ {":status","304"},
	        /* 12 */ {":status","400"},
	        /* 13 */ {":status","404"},
	        /* 14 */ {":status","500"},
	        /* 15 */ {"accept-charset",EMPTY},
	        /* 16 */ {"accept-encoding","gzip, deflate"},
	        /* 17 */ {"accept-language",EMPTY},
	        /* 18 */ {"accept-ranges",EMPTY},
	        /* 19 */ {"accept",EMPTY},
	        /* 20 */ {"access-control-allow-origin",EMPTY},
	        /* 21 */ {"age",EMPTY},
	        /* 22 */ {"allow",EMPTY},
	        /* 23 */ {"authorization",EMPTY},
	        /* 24 */ {"cache-control",EMPTY},
	        /* 25 */ {"content-disposition",EMPTY},
	        /* 26 */ {"content-encoding",EMPTY},
	        /* 27 */ {"content-language",EMPTY},
	        /* 28 */ {"content-length",EMPTY},
	        /* 29 */ {"content-location",EMPTY},
	        /* 30 */ {"content-range",EMPTY},
	        /* 31 */ {"content-type",EMPTY},
	        /* 32 */ {"cookie",EMPTY},
	        /* 33 */ {"date",EMPTY},
	        /* 34 */ {"etag",EMPTY},
	        /* 35 */ {"expect",EMPTY},
	        /* 36 */ {"expires",EMPTY},
	        /* 37 */ {"from",EMPTY},
	        /* 38 */ {"host",EMPTY},
	        /* 39 */ {"if-match",EMPTY},
	        /* 40 */ {"if-modified-since",EMPTY},
	        /* 41 */ {"if-none-match",EMPTY},
	        /* 42 */ {"if-range",EMPTY},
	        /* 43 */ {"if-unmodified-since",EMPTY},
	        /* 44 */ {"last-modified",EMPTY},
	        /* 45 */ {"link",EMPTY},
	        /* 46 */ {"location",EMPTY},
	        /* 47 */ {"max-forwards",EMPTY},
	        /* 48 */ {"proxy-authenticate",EMPTY},
	        /* 49 */ {"proxy-authorization",EMPTY},
	        /* 50 */ {"range",EMPTY},
	        /* 51 */ {"referer",EMPTY},
	        /* 52 */ {"refresh",EMPTY},
	        /* 53 */ {"retry-after",EMPTY},
	        /* 54 */ {"server",EMPTY},
	        /* 55 */ {"set-cookie",EMPTY},
	        /* 56 */ {"strict-transport-security",EMPTY},
	        /* 57 */ {"transfer-encoding",EMPTY},
	        /* 58 */ {"user-agent",EMPTY},
	        /* 59 */ {"vary",EMPTY},
	        /* 60 */ {"via",EMPTY},
	        /* 61 */ {"www-authenticate",EMPTY},
	    };

	private static final Map<HttpField, Entry> __staticFieldMap = new HashMap<>();
	private static final Trie<StaticEntry> __staticNameMap = new ArrayTernaryTrie<>(true, 512);
	private static final StaticEntry[] __staticTableByHeader = new StaticEntry[HttpHeader.UNKNOWN.ordinal()];
	private static final StaticEntry[] __staticTable = new StaticEntry[STATIC_TABLE.length];
	static {
		Set<String> added = new HashSet<>();
		for (int i = 1; i < STATIC_TABLE.length; i++) {
			StaticEntry entry = null;

			String name = STATIC_TABLE[i][0];
			String value = STATIC_TABLE[i][1];
			HttpHeader header = HttpHeader.CACHE.get(name);
			if (header != null && value != null) {
				switch (header) {
				case C_METHOD: {

					HttpMethod method = HttpMethod.CACHE.get(value);
					if (method != null)
						entry = new StaticEntry(i, new StaticTableHttpField(header, name, value, method));
					break;
				}

				case C_SCHEME: {

					HttpScheme scheme = HttpScheme.CACHE.get(value);
					if (scheme != null)
						entry = new StaticEntry(i, new StaticTableHttpField(header, name, value, scheme));
					break;
				}

				case C_STATUS: {
					entry = new StaticEntry(i, new StaticTableHttpField(header, name, value, Integer.valueOf(value)));
					break;
				}

				default:
					break;
				}
			}

			if (entry == null)
				entry = new StaticEntry(i,
						header == null ? new HttpField(STATIC_TABLE[i][0], value) : new HttpField(header, name, value));

			__staticTable[i] = entry;

			if (entry._field.getValue() != null)
				__staticFieldMap.put(entry._field, entry);

			if (!added.contains(entry._field.getName())) {
				added.add(entry._field.getName());
				__staticNameMap.put(entry._field.getName(), entry);
				if (__staticNameMap.get(entry._field.getName()) == null)
					throw new IllegalStateException("name trie too small");
			}
		}

		for (HttpHeader h : HttpHeader.values()) {
			StaticEntry entry = __staticNameMap.get(h.asString());
			if (entry != null)
				__staticTableByHeader[h.ordinal()] = entry;
		}
	}

	private int _maxDynamicTableSizeInBytes;
	private int _dynamicTableSizeInBytes;
	private final DynamicTable _dynamicTable;
	private final Map<HttpField, Entry> _fieldMap = new HashMap<>();
	private final Map<String, Entry> _nameMap = new HashMap<>();

	public HpackContext(int maxDynamicTableSize) {
		_maxDynamicTableSizeInBytes = maxDynamicTableSize;
		int guesstimateEntries = 10 + maxDynamicTableSize / (32 + 10 + 10);
		_dynamicTable = new DynamicTable(guesstimateEntries, guesstimateEntries + 10);
		if (log.isDebugEnabled())
			log.debug(String.format("HdrTbl[%x] created max=%d", hashCode(), maxDynamicTableSize));
	}

	public void resize(int newMaxDynamicTableSize) {
		if (log.isDebugEnabled())
			log.debug(String.format("HdrTbl[%x] resized max=%d->%d", hashCode(), _maxDynamicTableSizeInBytes,
					newMaxDynamicTableSize));
		_maxDynamicTableSizeInBytes = newMaxDynamicTableSize;
		int guesstimateEntries = 10 + newMaxDynamicTableSize / (32 + 10 + 10);
		evict();
		_dynamicTable.resizeUnsafe(guesstimateEntries);
	}

	public Entry get(HttpField field) {
		Entry entry = _fieldMap.get(field);
		if (entry == null)
			entry = __staticFieldMap.get(field);
		return entry;
	}

	public Entry get(String name) {
		Entry entry = __staticNameMap.get(name);
		if (entry != null)
			return entry;
		return _nameMap.get(StringUtils.asciiToLowerCase(name));
	}

	public Entry get(int index) {
		if (index < __staticTable.length)
			return __staticTable[index];

		int d = _dynamicTable.size() - index + __staticTable.length - 1;

		if (d >= 0)
			return _dynamicTable.getUnsafe(d);
		return null;
	}

	public Entry get(HttpHeader header) {
		Entry e = __staticTableByHeader[header.ordinal()];
		if (e == null)
			return get(header.asString());
		return e;
	}

	public static Entry getStatic(HttpHeader header) {
		return __staticTableByHeader[header.ordinal()];
	}

	public Entry add(HttpField field) {
		int slot = _dynamicTable.getNextSlotUnsafe();
		Entry entry = new Entry(slot, field);
		int size = entry.getSize();
		if (size > _maxDynamicTableSizeInBytes) {
			if (log.isDebugEnabled())
				log.debug(String.format("HdrTbl[%x] !added size %d>%d", hashCode(), size, _maxDynamicTableSizeInBytes));
			return null;
		}
		_dynamicTableSizeInBytes += size;
		_dynamicTable.addUnsafe(entry);
		_fieldMap.put(field, entry);
		_nameMap.put(StringUtils.asciiToLowerCase(field.getName()), entry);

		if (log.isDebugEnabled())
			log.debug(String.format("HdrTbl[%x] added %s", hashCode(), entry));
		evict();
		return entry;
	}

	/**
	 * @return Current dynamic table size in entries
	 */
	public int size() {
		return _dynamicTable.size();
	}

	/**
	 * @return Current Dynamic table size in Octets
	 */
	public int getDynamicTableSize() {
		return _dynamicTableSizeInBytes;
	}

	/**
	 * @return Max Dynamic table size in Octets
	 */
	public int getMaxDynamicTableSize() {
		return _maxDynamicTableSizeInBytes;
	}

	public int index(Entry entry) {
		if (entry._slot < 0)
			return 0;
		if (entry.isStatic())
			return entry._slot;

		return _dynamicTable.index(entry) + __staticTable.length - 1;
	}

	public static int staticIndex(HttpHeader header) {
		if (header == null)
			return 0;
		Entry entry = __staticNameMap.get(header.asString());
		if (entry == null)
			return 0;
		return entry.getSlot();
	}

	private void evict() {
		while (_dynamicTableSizeInBytes > _maxDynamicTableSizeInBytes) {
			Entry entry = _dynamicTable.pollUnsafe();
			if (log.isDebugEnabled())
				log.debug(String.format("HdrTbl[%x] evict %s", hashCode(), entry));
			_dynamicTableSizeInBytes -= entry.getSize();
			entry._slot = -1;
			_fieldMap.remove(entry.getHttpField());
			String lc = StringUtils.asciiToLowerCase(entry.getHttpField().getName());
			if (entry == _nameMap.get(lc))
				_nameMap.remove(lc);
		}
		if (log.isDebugEnabled())
			log.debug(String.format("HdrTbl[%x] entries=%d, size=%d, max=%d", hashCode(), _dynamicTable.size(),
					_dynamicTableSizeInBytes, _maxDynamicTableSizeInBytes));
	}

	@Override
	public String toString() {
		return String.format("HpackContext@%x{entries=%d,size=%d,max=%d}", hashCode(), _dynamicTable.size(),
				_dynamicTableSizeInBytes, _maxDynamicTableSizeInBytes);
	}

	private class DynamicTable extends ArrayQueue<HpackContext.Entry> {
		private DynamicTable(int initCapacity, int growBy) {
			super(initCapacity, growBy);
		}

		@Override
		protected void resizeUnsafe(int newCapacity) {
			// Relay on super.growUnsafe to pack all entries 0 to _nextSlot
			super.resizeUnsafe(newCapacity);
			for (int s = 0; s < _nextSlot; s++)
				((Entry) _elements[s])._slot = s;
		}

		@Override
		public boolean enqueue(Entry e) {
			return super.enqueue(e);
		}

		@Override
		public Entry dequeue() {
			return super.dequeue();
		}

		private int index(Entry entry) {
			return entry._slot >= _nextE ? _size - entry._slot + _nextE : _nextSlot - entry._slot;
		}
	}

	public static class Entry {
		final HttpField _field;
		int _slot;

		Entry() {
			_slot = 0;
			_field = null;
		}

		Entry(int index, String name, String value) {
			_slot = index;
			_field = new HttpField(name, value);
		}

		Entry(int slot, HttpField field) {
			_slot = slot;
			_field = field;
		}

		public int getSize() {
			String value = _field.getValue();
			return 32 + _field.getName().length() + (value == null ? 0 : value.length());
		}

		public HttpField getHttpField() {
			return _field;
		}

		public boolean isStatic() {
			return false;
		}

		public byte[] getStaticHuffmanValue() {
			return null;
		}

		public int getSlot() {
			return _slot;
		}

		public String toString() {
			return String.format("{%s,%d,%s,%x}", isStatic() ? "S" : "D", _slot, _field, hashCode());
		}
	}

	public static class StaticEntry extends Entry {
		private final byte[] _huffmanValue;
		private final byte _encodedField;

		StaticEntry(int index, HttpField field) {
			super(index, field);
			String value = field.getValue();
			if (value != null && value.length() > 0) {
				int huffmanLen = Huffman.octetsNeeded(value);
				int lenLen = NBitInteger.octectsNeeded(7, huffmanLen);
				_huffmanValue = new byte[1 + lenLen + huffmanLen];
				ByteBuffer buffer = ByteBuffer.wrap(_huffmanValue);

				// Indicate Huffman
				buffer.put((byte) 0x80);
				// Add huffman length
				NBitInteger.encode(buffer, 7, huffmanLen);
				// Encode value
				Huffman.encode(buffer, value);
			} else
				_huffmanValue = null;

			_encodedField = (byte) (0x80 | index);
		}

		@Override
		public boolean isStatic() {
			return true;
		}

		@Override
		public byte[] getStaticHuffmanValue() {
			return _huffmanValue;
		}

		public byte getEncodedField() {
			return _encodedField;
		}
	}
}
