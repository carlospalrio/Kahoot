package com.example.m8_kahoot_server;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.serverkahoooot.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference myRef;
    private FirebaseDatabase database;
    private TextView partyNumTextView;
    private ListView listViewUsuarios;
    private ArrayAdapter<String> adaptador;
    private ArrayList<String> listaUsuarios;
    private String numeroSala;
    private Button startQuizButton;

    // Método para generar un número de sala aleatorio
    private String generarNumeroSala() {
        Random random = new Random();
        int numero = random.nextInt(9999 - 1) + 1;
        return String.format("%04d", numero);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        partyNumTextView = findViewById(R.id.partyNum);
        listViewUsuarios = findViewById(R.id.listViewUsuarios);
        startQuizButton = findViewById(R.id.startQuiz);

        // Inicializar Firebase
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();


        listaUsuarios = new ArrayList<>();
        adaptador = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaUsuarios);
        listViewUsuarios.setAdapter(adaptador);

        // Generar la sala
        generarSala();

        // Botón para iniciar el quiz
        startQuizButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, QuizActivity.class);
                intent.putExtra("numeroSalaaa", numeroSala);
                startActivity(intent);
            }
        });

        // Márgenes
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Generar la sala y guardarla en Firebase
    private void generarSala() {
        String numeroFormateado = generarNumeroSala();

        // Consultar si la sala ya existe en Firebase
        myRef.child(numeroFormateado).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d("MiTag", "Sala " + numeroFormateado + " ya existe. Generando otra...");
                    generarSala(); // Si existe, generamos otra sala
                } else {
                    // Guardamos el número de sala generado
                    numeroSala = numeroFormateado;
                    myRef.child(numeroSala).child("usuarios").setValue("");  // Inicializa el nodo "usuarios"
                    myRef.child(numeroSala).child("quiz").setValue(0);  // Inicializa el estado del quiz
                    myRef.child(numeroSala).child("total").setValue(0);  // Inicializa el puntaje total
                    myRef.child(numeroSala).child("ganadores").setValue(0);  // Inicializa los ganadores

                    // Mostrar la sala en el TextView
                    partyNumTextView.setText(numeroSala);
                    Log.d("MiTag", "Sala creada con ID: " + numeroSala);

                    // Escuchar la lista de usuarios conectados
                    escucharUsuarios();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("MiTag", "Error al acceder a Firebase: " + databaseError.getMessage());
            }
        });
    }

    // Ver los usuarios conectados en la sala
    private void escucharUsuarios() {
        if (numeroSala == null) return;

        myRef.child(numeroSala).child("usuarios").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                listaUsuarios.clear(); // Limpiar lista antes de actualizar

                // Recorrer la lista de usuarios y agregar a la UI
                for (DataSnapshot usuarioSnapshot : snapshot.getChildren()) {
                    String nombreUsuario = usuarioSnapshot.getKey(); // TOMAMOS LA CLAVE
                    if (nombreUsuario != null) {
                        listaUsuarios.add(nombreUsuario);
                        Log.d("FirebaseData", "Usuario agregado: " + nombreUsuario);
                    }
                }
                adaptador.notifyDataSetChanged(); // Refrescar ListView

                Log.d("FirebaseData", "Lista actualizada con: " + listaUsuarios.size() + " usuarios.");
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.d("MiTag", "Error al leer usuarios: " + error.getMessage());
            }
        });
    }

}
