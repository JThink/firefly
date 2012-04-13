package test.utils.json.parser;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import test.utils.json.ArrayObj;
import test.utils.json.Book;
import test.utils.json.CollectionObj;
import test.utils.json.MapObj;
import test.utils.json.SimpleObj;
import test.utils.json.SimpleObj2;
import test.utils.json.User;
import test.utils.json.github.MediaContent;
import test.utils.json.github.Player;
import test.utils.json.github.Size;

import com.firefly.utils.json.Json;

public class TestParser {
	@Test
	public void test() {
		SimpleObj i = new SimpleObj();
		i.setAge(10);
		i.setId(33442);
		i.setNumber(30);
		i.setName("PengtaoQiu\nAlvin");
		i.setType((short)-33);
		i.setWeight(55.47f);
		i.setHeight(170.5);
		String jsonStr = Json.toJson(i);
		
		SimpleObj i2 = Json.toObject(jsonStr, SimpleObj.class);
		Assert.assertThat(i2.getAge(), is(10));
		Assert.assertThat(i2.getId(), is(33442));
		Assert.assertThat(i2.getNumber(), is(30));
		Assert.assertThat(i2.getDate(), is(0L));
		Assert.assertThat(i2.getName(), is("PengtaoQiu\nAlvin"));
		Assert.assertThat(i2.getType(), is((short)-33));
		Assert.assertThat(i2.getHeight(), is(170.5));
		Assert.assertThat(i2.getWeight(), is(55.47f));
	}
	
	@Test
	public void test2() {
		SimpleObj i = new SimpleObj();
		i.setAge(10);
		i.setId(33442);
		i.setNumber(30);
		i.setName("PengtaoQiu\nAlvin");
		
		SimpleObj i2 = new SimpleObj();
		i2.setAge(20);
		i2.setId(12341);
		i2.setNumber(33);
		i2.setName("Tom");
		i.setContact1(i2);
		String jsonStr = Json.toJson(i);
		
		SimpleObj temp = Json.toObject(jsonStr, SimpleObj.class);
		Assert.assertThat(temp.getId(), is(33442));
		Assert.assertThat(temp.getContact1().getId(), is(12341));
		Assert.assertThat(temp.getContact1().getName(), is("Tom"));
		Assert.assertThat(temp.getContact1().getAge(), is(20));
		Assert.assertThat(temp.getContact2(), nullValue());
	}
	
	@Test
	public void test3() {
		String jsonStr = "{\"id\":33442,\"date\":null,\"add1\":{}, \"add2\":{}, \"add3\":{}, \"add4\":{}, \"add5\":null,\"add6\":\"sdfsdf\",\"contact2\":{}, \"number\":30,\"height\":null,\"name\":\"PengtaoQiu\nAlvin\",\"type\":null,\"weight\":40.3}";
		SimpleObj temp = Json.toObject(jsonStr, SimpleObj.class);
		Assert.assertThat(temp.getName(), is("PengtaoQiu\nAlvin"));
		Assert.assertThat(temp.getId(), is(33442));
		Assert.assertThat(temp.getWeight(), is(40.3F));
	}
	
	@Test
	public void test4() {
		SimpleObj2 so2 = new SimpleObj2();
		so2.setId(334);
		
		User user = new User();
		user.setId(2434L);
		user.setName("Pengtao");
		so2.setUser(user);
		
		Book book = new Book();
		book.setId(23424);
		book.setPrice(3.4);
		book.setSell(true);
		book.setText("cccccccc");
		book.setTitle("ddddd");
		so2.setBook(book);
		
		String jsonStr = Json.toJson(so2);
		
		SimpleObj2 temp = Json.toObject(jsonStr, SimpleObj2.class);
		Assert.assertThat(temp.getBook().getPrice(), is(3.4));
		Assert.assertThat(temp.getBook().getTitle(), nullValue());
		Assert.assertThat(temp.getId(), is(334));
	}
	
	@Test
	public void test5() {
		List<LinkedList<SimpleObj>> list = new LinkedList<LinkedList<SimpleObj>>();
		
		LinkedList<SimpleObj> list1 = new LinkedList<SimpleObj>();
		for (int j = 0; j < 10; j++) {
			SimpleObj i = new SimpleObj();
			i.setAge(10);
			i.setId(33442 + j);
			i.setNumber(30);
			i.setName("PengtaoQiu\nAlvin");
			
			SimpleObj i2 = new SimpleObj();
			i2.setAge(20);
			i2.setId(12341);
			i2.setNumber(33);
			i2.setName("Tom");
			i.setContact1(i2);
			list1.add(i);
		}
		list.add(list1);
		
		list1 = new LinkedList<SimpleObj>();
		for (int j = 0; j < 10; j++) {
			SimpleObj i = new SimpleObj();
			i.setAge(10);
			i.setId(1000 + j);
			i.setNumber(30);
			i.setName("PengtaoQiu\nAlvin");
			
			SimpleObj i2 = new SimpleObj();
			i2.setAge(20);
			i2.setId(12341);
			i2.setNumber(33);
			i2.setName("Tom");
			i.setContact1(i2);
			list1.add(i);
		}
		list.add(list1);
		
		CollectionObj o = new CollectionObj();
		o.setList(list);
		String json = Json.toJson(o);
		
		CollectionObj o2 = Json.toObject(json, CollectionObj.class);
		Assert.assertThat(o2.getList().size(), is(2));
		Assert.assertThat(o2.getList().get(0).size(), is(10));
		Assert.assertThat(o2.getList().get(0).get(1).getId(), is(33443));
		Assert.assertThat(o2.getList().get(1).get(1).getId(), is(1001));
	}
	
	@Test
	public void test6() {
		 MediaContent record = MediaContent.createRecord();
		 String json = Json.toJson(record);
	     
	     MediaContent r = Json.toObject(json, MediaContent.class);
	     Assert.assertThat(r.getMedia().getPlayer(), is(Player.JAVA));
	     Assert.assertThat(r.getImages().size(), is(2));
	     Assert.assertThat(r.getImages().get(0).getSize(), is(Size.LARGE));
	     Assert.assertThat(r.getImages().get(0).getHeight(), is(768));
	}
	
	@Test
	public void test7() {
		ArrayObj obj = new ArrayObj();
		Integer[] i = new Integer[]{2,3,4,5,6,332};
		obj.setNumbers(i);
		
		long[][] map = new long[][]{{3L, 44L, 55L}, {24, 324, 3}};
		obj.setMap(map);
		
		List<User> users = new ArrayList<User>();
		for (int j = 0; j < 3; j++) {
			User user = new User();
			user.setId((long)j);
			user.setName("user" + j);
			users.add(user);
		}
		obj.setUsers(users.toArray(new User[0]));
		
		String json = Json.toJson(obj);
		
		ArrayObj obj2 = Json.toObject(json, ArrayObj.class);
		Assert.assertThat(obj2.getNumbers()[3], is(5));
		Assert.assertThat(obj2.getNumbers().length, is(6));
		Assert.assertThat(obj2.getMap().length, is(2));
		Assert.assertThat(obj2.getMap()[0][1], is(44L));
		Assert.assertThat(obj2.getUsers().length, is(3));
		Assert.assertThat(obj2.getUsers()[0].getId(), is(0L));
		Assert.assertThat(obj2.getUsers()[1].getName(), is("user1"));
	}
	
	@Test
	public void test8() {
		List<User> users = new ArrayList<User>();
		for (int j = 0; j < 3; j++) {
			User user = new User();
			user.setId((long)j);
			user.setName("user" + j);
			users.add(user);
		}
		User[] u = users.toArray(new User[0]);
		String json = Json.toJson(u);
		
		User[] u2 = Json.toObject(json, User[].class);
		Assert.assertThat(u2.length, is(3));
		Assert.assertThat(u2[0].getId(), is(0L));
		Assert.assertThat(u2[1].getName(), is("user1"));
	}
	
	@Test
	public void test9() {
		MapObj m = new MapObj();
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("a1", 40);
		m.setMap(map);
		
		Map<String, User[]> userMap = new HashMap<String, User[]>();
		List<User> users = new ArrayList<User>();
		for (int j = 0; j < 3; j++) {
			User user = new User();
			user.setId((long)j);
			user.setName("user" + j);
			users.add(user);
		}
		User[] u = users.toArray(new User[0]);
		userMap.put("user1", u);
		
		users = new ArrayList<User>();
		for (int j = 10; j < 12; j++) {
			User user = new User();
			user.setId((long)j);
			user.setName("user_b" + j);
			users.add(user);
		}
		u = users.toArray(new User[0]);
		userMap.put("user2", u);
		m.setUserMap(userMap);
		
		Map<String, int[]> map3 = new HashMap<String, int[]>();
		map3.put("m31", new int[]{3,4,5,6});
		map3.put("m32", new int[]{7,8,9});
		m.setMap3(map3);
		
		String json = Json.toJson(m);
		
		MapObj m2 = Json.toObject(json, MapObj.class);
		Assert.assertThat(m2.getMap().get("a1"), is(40));
		Assert.assertThat(m.getUserMap().get("user1").length, is(3));
		Assert.assertThat(m.getUserMap().get("user2").length, is(2));
		Assert.assertThat(m.getUserMap().get("user2")[0].getName(), is("user_b10"));
		Assert.assertThat(m2.getMap3().get("m31")[3], is(6));
	}
	
	public static void main(String[] args) throws Throwable {
		MapObj m = new MapObj();
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("a1", 40);
		m.setMap(map);
		
		Map<String, User[]> userMap = new HashMap<String, User[]>();
		List<User> users = new ArrayList<User>();
		for (int j = 0; j < 3; j++) {
			User user = new User();
			user.setId((long)j);
			user.setName("user" + j);
			users.add(user);
		}
		User[] u = users.toArray(new User[0]);
		userMap.put("user1", u);
		
		users = new ArrayList<User>();
		for (int j = 10; j < 12; j++) {
			User user = new User();
			user.setId((long)j);
			user.setName("user_b" + j);
			users.add(user);
		}
		u = users.toArray(new User[0]);
		userMap.put("user2", u);
		m.setUserMap(userMap);
		
		Map<String, int[]> map3 = new HashMap<String, int[]>();
		map3.put("m31", new int[]{3,4,5,6});
		map3.put("m32", new int[]{7,8,9});
		m.setMap3(map3);
		
		String json = Json.toJson(m);
		System.out.println(json);
		
		MapObj m2 = Json.toObject(json, MapObj.class);
		System.out.println(m2.getUserMap().get("user2")[0].getName());
		System.out.println(m2.getMap3().get("m31")[3]);
	}
}