package com.example.m8_kahoot_server;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.serverkahoooot.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ResultadosActivity extends AppCompatActivity {
    private LinearLayout layoutUsuarios, layoutGanador;
    private DatabaseReference dbRef;
    private String numeroSala;
    private Map<String, String> respuestasCorrectas = new HashMap<>();
    private Map<String, Integer> puntuaciones = new HashMap<>();
    private List<String> ganadores = new ArrayList<>();
    private CheckBox checkBoxMostrarRespuestas;
    private Button btnVolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultados);

        layoutUsuarios = findViewById(R.id.layoutUsuarios);
        layoutGanador = findViewById(R.id.layoutGanador);
        checkBoxMostrarRespuestas = findViewById(R.id.checkBoxMostrarRespuestas);
        btnVolver = findViewById(R.id.btnVolver);

        numeroSala = getIntent().getStringExtra("numeroSalaaa"); // Recibe el número de sala

        if (numeroSala == null) {
            Toast.makeText(this, "Error: No se recibió el número de sala", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbRef = FirebaseDatabase.getInstance().getReference();
        cargarRespuestasCorrectas();

        // Establecer el listener para la CheckBox
        checkBoxMostrarRespuestas.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Mostrar los usuarios con sus respuestas
                cargarUsuariosYComparar();
            } else {
                layoutUsuarios.setVisibility(View.GONE);
                mostrarGanadores();
            }
        });

        if (!checkBoxMostrarRespuestas.isChecked()) {
            cargarGanadoresSolo();
        }

        // Configurar el botón "Volver" para regresar a la pantalla inicial
        btnVolver.setOnClickListener(v -> volverAPantallaInicial());
    }

    private void cargarRespuestasCorrectas() {
        dbRef.child("Correctas").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                respuestasCorrectas.clear();

                for (DataSnapshot preguntaSnapshot : snapshot.getChildren()) {
                    respuestasCorrectas.put(preguntaSnapshot.getKey(), preguntaSnapshot.getValue(String.class));
                }

                // Si el checkbox está marcado, mostrar las respuestas correctas y los usuarios
                if (checkBoxMostrarRespuestas.isChecked()) {
                    cargarUsuariosYComparar();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseData", "Error al leer respuestas correctas: " + error.getMessage());
            }
        });
    }

    private void cargarUsuariosYComparar() {
        dbRef.child(numeroSala).child("usuarios").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                layoutUsuarios.removeAllViews();
                puntuaciones.clear();

                for (DataSnapshot usuarioSnapshot : snapshot.getChildren()) {
                    String nombreUsuario = usuarioSnapshot.getKey();
                    if (nombreUsuario != null) {
                        int puntaje = compararRespuestas(usuarioSnapshot);
                        puntuaciones.put(nombreUsuario, puntaje);
                        mostrarUsuario(nombreUsuario, usuarioSnapshot);
                    }
                }

                determinarGanadores();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseData", "Error al leer usuarios: " + error.getMessage());
            }
        });
    }

    private int compararRespuestas(DataSnapshot usuarioSnapshot) {
        int aciertos = 0;
        for (DataSnapshot respuestaSnapshot : usuarioSnapshot.getChildren()) {
            String pregunta = respuestaSnapshot.getKey();
            String respuestaUsuario = respuestaSnapshot.getValue(String.class);

            if (pregunta != null && respuestaUsuario != null && respuestaUsuario.equals(respuestasCorrectas.get(pregunta))) {
                aciertos++;
            }
        }
        return aciertos;
    }

    private void mostrarUsuario(String nombre, DataSnapshot usuarioSnapshot) {
        TextView textView = new TextView(this);
        textView.setText("Usuario: " + nombre);
        textView.setTextSize(18);
        textView.setPadding(10, 10, 10, 10);
        layoutUsuarios.addView(textView);

        for (DataSnapshot datoSnapshot : usuarioSnapshot.getChildren()) {
            String key = datoSnapshot.getKey();
            String value = datoSnapshot.getValue(String.class);

            if (key != null && value != null) {
                TextView textDato = new TextView(this);
                textDato.setText(" - " + key + ": " + value);
                textDato.setTextSize(16);
                textDato.setPadding(20, 5, 10, 5);
                layoutUsuarios.addView(textDato);
            }
        }
    }



    private void determinarGanadores() {
        int maxPuntos = -1;
        ganadores.clear();

        // Determinamos el puntaje máximo
        for (Map.Entry<String, Integer> entry : puntuaciones.entrySet()) {
            int puntos = entry.getValue();
            String nombre = entry.getKey();

            if (puntos > maxPuntos) {
                maxPuntos = puntos;
                ganadores.clear(); // Limpiamos la lista si encontramos un nuevo máximo
                ganadores.add(nombre); // Agregamos al nuevo ganador
            } else if (puntos == maxPuntos) {
                ganadores.add(nombre); // Agregamos al empate
            }
        }

        mostrarGanadores();
        actualizarGanadoresEnFirebase();
    }

    private void mostrarGanadores() {
        layoutGanador.removeAllViews();
        TextView textView = new TextView(this);

        if (ganadores.size() == 1) {
            textView.setText("Ganador: " + ganadores.get(0));
        } else {
            StringBuilder ganadoresTexto = new StringBuilder("Jugadores: ");
            for (String ganador : ganadores) {
                ganadoresTexto.append(ganador).append(", ");
            }
            ganadoresTexto.setLength(ganadoresTexto.length() - 2); // Eliminar la última coma y espacio
            textView.setText(ganadoresTexto.toString());
        }

        textView.setTextSize(20);
        textView.setPadding(10, 10, 10, 10);
        layoutGanador.addView(textView);
    }

    // Función para actualizar los ganadores en Firebase
    private void actualizarGanadoresEnFirebase() {
        if (!ganadores.isEmpty()) {
            dbRef.child(numeroSala).child("ganadores").setValue(ganadores)
                    .addOnSuccessListener(aVoid -> Log.d("FirebaseData", "Ganadores actualizados en Firebase"))
                    .addOnFailureListener(e -> Log.e("FirebaseData", "Error al actualizar ganadores: " + e.getMessage()));
        }
    }

    // Función para volver a la pantalla inicial y crear una nueva sala
    private void volverAPantallaInicial() {
        // Eliminar la sala actual en Firebase
        dbRef.child(numeroSala).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Generar un nuevo código de sala
                    String nuevoNumeroSala = generarNumeroSala();

                    // Redirigir a MainActivity con el nuevo número de sala
                    Intent intent = new Intent(ResultadosActivity.this, MainActivity.class);
                    intent.putExtra("numeroSalaaa", nuevoNumeroSala);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ResultadosActivity.this, "Error al eliminar la sala", Toast.LENGTH_SHORT).show();
                });
    }

    // Generar un nuevo código aleatorio para la sala
    private String generarNumeroSala() {
        Random random = new Random();
        int numero = random.nextInt(9999 - 1) + 1; // Genera entre 1 y 9999
        return String.format("%04d", numero); // Formatea a 4 dígitos
    }

    // Cargar solo los ganadores para mostrarlos despues
    private void cargarGanadoresSolo() {
        dbRef.child(numeroSala).child("usuarios").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                puntuaciones.clear();

                // Solo calculamos los ganadores sin mostrar usuarios ni respuestas
                for (DataSnapshot usuarioSnapshot : snapshot.getChildren()) {
                    String nombreUsuario = usuarioSnapshot.getKey();
                    if (nombreUsuario != null) {
                        int puntaje = compararRespuestas(usuarioSnapshot);
                        puntuaciones.put(nombreUsuario, puntaje);
                    }
                }

                determinarGanadores();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseData", "Error al leer usuarios: " + error.getMessage());
            }
        });
    }
}
