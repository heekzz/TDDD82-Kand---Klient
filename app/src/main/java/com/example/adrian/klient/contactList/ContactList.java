package com.example.adrian.klient.contactList;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.adrian.klient.R;
import com.example.adrian.klient.ServerConnection.CONN;
import com.example.adrian.klient.ServerConnection.Request;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;

public class ContactList extends AppCompatActivity {
    ArrayAdapter<String> adapter;
    CONN connection;
    ArrayList contactList;
    int permission;
    JsonArray data = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        ListView listView = (ListView)findViewById(R.id.contactList);
        // Arraylist to store the contacts
        contactList = new ArrayList<>();

        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,contactList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(ContactList.this, Contact.class);
                Contact pressed = (Contact) contactList.get(position);

                intent.putExtra("PERMISSION", pressed.getPermission());
                intent.putExtra("NAME", pressed.get_name());
                intent.putExtra("SSN", pressed.getSSN());
                intent.putExtra("ADDRESS", pressed.getAddress());
                intent.putExtra("PHONE_NR", pressed.getPhoneNr());
                intent.putExtra("TITLE", pressed.getWorkTitle());
                intent.putExtra("SALARY", pressed.getSalary());
                startActivity(intent);
            }
        });

        getContacts();

    }

    public void getContacts (){
        new Request(this,"get").contactRequest();
        connection = new CONN(this);
        new Thread(connection).start();

        //Get response from server
        do{
            data = connection.getData();
        } while(data == null);

        permission = connection.getPermission();
        System.out.println("data: " + data);

        contactList = new ArrayList<>();

        //Add the contacts to the list
        int pos = 0;
        for(JsonElement e : data){
            JsonObject o = e.getAsJsonObject();
            String title, salary, ssn, address;
            Contact contact = new Contact();

            //Put the contact info on to current contact
            String name = o.get("name").getAsString();
            String phoneNr = o.get("phonenr").getAsString();

            //All permission fields
            contact.setPermission(permission);
            contact.set_name(name);
            contact.setPhoneNr(phoneNr);

            switch (permission){
                case 1:
                    // Mid permission fields
                    title = o.get("title").getAsString();
                    address = o.get("address").getAsString();
                    contact.setWorkTitle(title);
                    contact.setAddress(address);
                    break;
                case 2:
                    // Highest permission fields
                    address = o.get("address").getAsString();
                    contact.setAddress(address);
                    title = o.get("title").getAsString();
                    contact.setWorkTitle(title);
                    ssn = o.get("ssn").getAsString();
                    contact.setSSN(ssn);
                    salary = o.get("salary").getAsString();
                    contact.setSalary(salary);
                    break;
                default:
                    // Lowest priority gets only name, address and phoneNr-fields
            }
            adapter.add(name);
            contactList.add(pos,contact);
            pos++;

        }
    }
}
