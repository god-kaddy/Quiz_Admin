package com.example.quizadmin;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CategoryActivity extends AppCompatActivity {

    private RecyclerView cat_recycler_view;
    private Button addCatB;
    public static List<CategoryModel> catList = new ArrayList<>();
    public static int selected_cat_index=0;

    private FirebaseFirestore firestore;
    private Dialog loadingDialog, addCatDialog;
    private EditText dialogCatName;
    private Button dialogAddB;
    private CategoryAdapter adapter;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Categories");

        //Toolbar toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        //getSupportActionBar().setTitle("Categories");


        cat_recycler_view = findViewById(R.id.cat_recycler);
        addCatB = findViewById(R.id.addCatB);

        loadingDialog = new Dialog(CategoryActivity.this);
        loadingDialog.setContentView(R.layout.loading_progressbar);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawableResource(R.drawable.progress_background);
        loadingDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        addCatDialog = new Dialog(CategoryActivity.this);
        addCatDialog.setContentView(R.layout.add_category_dialog);
        addCatDialog.setCancelable(true);
        addCatDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        dialogCatName = addCatDialog.findViewById(R.id.ac_cat_name);
        dialogAddB = addCatDialog.findViewById(R.id.ac_add_btn);

        /*List<String> catList = new ArrayList<>();
        catList.add("CAT 1");
        catList.add("CAT 2");
        catList.add("CAT 3");
        catList.add("CAT 4");*/

        firestore = FirebaseFirestore.getInstance();

        addCatB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogCatName.getText().clear();
                addCatDialog.show();
            }
        });

        dialogAddB.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                if(dialogCatName.getText().toString().isEmpty())
                {
                    dialogCatName.setError("Enter Category Name");
                    return;
                }

                addNewCategory(dialogCatName.getText().toString());
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        cat_recycler_view.setLayoutManager(layoutManager);

        CategoryAdapter adapter = new CategoryAdapter(catList);
        cat_recycler_view.setAdapter(adapter);

        loadData();;

    }

   // private void setSupportActionBar(Toolbar toolbar) {
    //}

    private void loadData()
    {
        loadingDialog.show();

        catList.clear();

        firestore.collection("QUIZ").document("Categories")
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if(task.isSuccessful())
                {
                    DocumentSnapshot doc = task.getResult();

                    if(doc.exists())
                    {
                        long count = (long)doc.get("COUNT");

                        for(int i=1; i <= count; i++)
                        {
                            String catName = doc.getString("CAT" + String.valueOf(i) + "_NAME");
                            String catid = doc.getString("CAT" + String.valueOf(i) + "_ID");

                            catList.add(new CategoryModel(catid,catName,"0","1"));
                        }


                        adapter = new CategoryAdapter(catList);
                        cat_recycler_view.setAdapter(adapter);

                    }
                    else
                    {
                        Toast.makeText(CategoryActivity.this,"No Category Document Exists!",Toast.LENGTH_SHORT).show();
                        finish();
                    }

                }
                else
                {
                    Toast.makeText(CategoryActivity.this,task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                }

                loadingDialog.dismiss();
            }
        });

    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void addNewCategory(final String title) {
        addCatDialog.dismiss();
        loadingDialog.show();

        final Map<String, Object> catData = new ArrayMap<>();
        //Map<String,Object> catData = new ArrayMap<>();
        catData.put("NAME", title);
        catData.put("SETS", 0);
        catData.put("COUNTER", "1");

        final String doc_id = firestore.collection("QUIZ").document().getId();

        firestore.collection("QUIZ").document(doc_id)
                .set(catData)
                .addOnSuccessListener(new OnSuccessListener<Void>()
                {
                    @Override
                    public void onSuccess(Void aVoid)
                    {
                        Map<String, Object> catDoc = new ArrayMap<>();
                        catDoc.put("CAT" + String.valueOf(catList.size() + 1) + "_NAME", title);
                        catDoc.put("CAT" + String.valueOf(catList.size() + 1) + "_ID", doc_id);
                        catDoc.put("COUNT", catList.size() + 1);

                        firestore.collection("QUIZ").document("Categories")
                                .update(catDoc)
                                .addOnSuccessListener(new OnSuccessListener<Void>()
                                {
                                    @Override
                                    public void onSuccess(Void aVoid)
                                    {

                                        Toast.makeText(CategoryActivity.this, "Category added successfully", Toast.LENGTH_SHORT).show();

                                        catList.add(new CategoryModel(doc_id, title, "0", "1"));

                                        adapter.notifyItemInserted(catList.size());

                                        loadingDialog.dismiss();

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener()
                                {
                                    @Override
                                    public void onFailure(@NonNull Exception e)
                                    {
                                        Toast.makeText(CategoryActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        loadingDialog.dismiss();
                                    }
                                });


                    }
                })
                .addOnFailureListener(new OnFailureListener()
                {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CategoryActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        loadingDialog.dismiss();
                    }
                });


    }
    }