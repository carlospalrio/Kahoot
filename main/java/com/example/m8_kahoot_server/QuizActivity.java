package com.example.m8_kahoot_server;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

public class QuizActivity extends AppCompatActivity {

    private TextView textPregunta;
    private Button btnA, btnB, btnC, btnD, btnSiguiente, endQuizz;

    private DatabaseReference preguntasRef;
    private int numeroPregunta = 1;
    private static final int TOTAL_PREGUNTAS = 3; // Solo hay 2 preguntas

    String numeroSala;
    int numeroQuiz = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        Intent intent = getIntent();
        numeroSala = intent.getStringExtra("numeroSalaaa");

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef1 = database.getReference(numeroSala + "/quiz");
        myRef1.setValue(numeroQuiz);

        textPregunta = findViewById(R.id.textPregunta);
        btnA = findViewById(R.id.btnA);
        btnB = findViewById(R.id.btnB);
        btnC = findViewById(R.id.btnC);
        btnD = findViewById(R.id.btnD);
        btnSiguiente = findViewById(R.id.btnSiguiente);
        endQuizz = findViewById(R.id.endQuizz);

        preguntasRef = FirebaseDatabase.getInstance().getReference().child("Preguntas");

        cargarPregunta(numeroPregunta);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button botonPresionado = (Button) v;
                mostrarOpcionSeleccionada(botonPresionado.getText().toString());
            }
        };

        btnA.setOnClickListener(listener);
        btnB.setOnClickListener(listener);
        btnC.setOnClickListener(listener);
        btnD.setOnClickListener(listener);

        btnSiguiente.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                siguientePregunta();
            }
        });

        endQuizz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Marcar el quiz como terminado en Firebase
                DatabaseReference myRef2 = FirebaseDatabase.getInstance().getReference(numeroSala + "/quiz");
                myRef2.setValue(-1); // Indica que el quiz ha terminado
                irAResultados();
            }
        });

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                siguientePregunta();
                handler.postDelayed(this, 30000);
            }
        };
        handler.postDelayed(runnable, 30000);

        // Agregar listener para verificar el fin del quiz desde Firebase
        DatabaseReference quizRef = database.getReference(numeroSala + "/quiz");
        quizRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Object value = snapshot.getValue();
                    if (value != null) {
                        int estadoQuiz = Integer.parseInt(value.toString());
                        if (estadoQuiz == -1) {
                            irAResultados();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(QuizActivity.this, "Error al verificar estado del quiz", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarPregunta(int numeroPregunta) {
        Log.d("QuizActivity", "Cargando pregunta número: " + numeroPregunta);
        preguntasRef.child(String.valueOf(numeroPregunta)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String pregunta = dataSnapshot.child("0").getValue(String.class);
                    String opcionA = dataSnapshot.child("A").getValue(String.class);
                    String opcionB = dataSnapshot.child("B").getValue(String.class);
                    String opcionC = dataSnapshot.child("C").getValue(String.class);
                    String opcionD = dataSnapshot.child("D").getValue(String.class);

                    textPregunta.setText(pregunta);
                    btnA.setText(opcionA);
                    btnB.setText(opcionB);
                    btnC.setText(opcionC);
                    btnD.setText(opcionD);
                } else {
                    Log.d("QuizActivity", "Pregunta no encontrada en Firebase.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(QuizActivity.this, "Error al cargar la pregunta", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void mostrarOpcionSeleccionada(String opcionSeleccionada) {
        Toast.makeText(this, "Opción seleccionada: " + opcionSeleccionada, Toast.LENGTH_SHORT).show();
    }

    private void siguientePregunta() {
        if (numeroPregunta >= TOTAL_PREGUNTAS) {
            // Marcar fin del quiz en Firebase
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef2 = database.getReference(numeroSala + "/quiz");
            myRef2.setValue(-1); // Indica que el quiz ha terminado
            irAResultados();
            return;
        }

        // Continuar con la siguiente pregunta si aún no es la última
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef2 = database.getReference(numeroSala + "/quiz");

        numeroQuiz++;
        myRef2.setValue(numeroQuiz);

        numeroPregunta++;
        cargarPregunta(numeroPregunta);
    }

    private void irAResultados() {
        Intent intent = new Intent(QuizActivity.this, ResultadosActivity.class);
            intent.putExtra("numeroSalaaa", numeroSala);
        startActivity(intent);
        finish(); // Cierra esta actividad para evitar volver atrás
    }
}
