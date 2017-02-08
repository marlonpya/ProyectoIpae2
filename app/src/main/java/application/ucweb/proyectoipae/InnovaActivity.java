package application.ucweb.proyectoipae;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.transition.Slide;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import application.ucweb.proyectoipae.aplicacion.BaseActivity;
import application.ucweb.proyectoipae.aplicacion.Configuracion;
import application.ucweb.proyectoipae.model.Innova;
import application.ucweb.proyectoipae.model.PreguntaInnova;
import application.ucweb.proyectoipae.model.Test;
import application.ucweb.proyectoipae.realm.InnovaRealmAdapter;
import application.ucweb.proyectoipae.realm.MetodoServices;
import application.ucweb.proyectoipae.util.ConexionBroadcastReceiver;
import application.ucweb.proyectoipae.util.Constantes;
import application.ucweb.proyectoipae.util.Preferencia;
import butterknife.BindView;
import co.moonmonkeylabs.realmrecyclerview.RealmRecyclerView;
import io.realm.Realm;
import io.realm.RealmResults;

public class InnovaActivity extends BaseActivity {
    public static final String TAG = InnovaActivity.class.getSimpleName();
    @BindView(R.id.toolbar_principal) Toolbar toolbar;
    @BindView(R.id.listaRecyclerInnova) RealmRecyclerView listaRecyclerInnova;
    @BindView(R.id.iv_tarjeta_innova_lista) ImageView cabeceraInnova;
    @BindView(R.id.ivToolbar_Icono) ImageView icono_toolbar;
    @BindView(R.id.layout_a_innova) LinearLayout layout;
    private RealmResults<Innova> listaInnova;
    private InnovaRealmAdapter realmAdapter;
    private Realm realm;
    private ProgressDialog pDialog;
    private boolean preguntas_por_enviar_i = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            hacerTransicion();
        }
        super.onCreate(savedInstanceState);
        FirebaseMessaging.getInstance().subscribeToTopic("ipae");

        setContentView(R.layout.activity_innova);
        setToolbarSon(toolbar, this, icono_toolbar);
        iniciarLayout();

        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Generando test..");
        pDialog.setCancelable(false);

        //VARIABLE QUE DEFINE SI ES QUE NO HA ENVIADO AUN TODAS LAS PREGUNTAS
        preguntas_por_enviar_i = getSharedPreferences(Constantes.PREFERENCIA_PARA_PREGUNTAS, Context.MODE_PRIVATE).getBoolean(Constantes.KEY_ESTADO_DIAGNOSTICO_INNOVA, false);
        if (preguntas_por_enviar_i) { mostrarMensajePreguntasNoEnviadas(this, 2, this); }
        Log.d(TAG, "diagnostico_activado_" + String.valueOf(preguntas_por_enviar_i));

        if (!Test.getTest(2).isEstado_token()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String fecha = sdf.format(new Date());
            int id_categoria = Test.getTest(2).getId_categoria();
            String token = Test.getTest(2).getToken_test();
            int idPeriodo = realm.where(Innova.class).max(Innova.I_ID_PERIODO).intValue();
            generarToken(fecha, id_categoria, token, idPeriodo);
            Log.d(TAG, "generarToken/fecha_"+fecha);
            Log.d(TAG, "generarToken/idCategoria_"+String.valueOf(id_categoria));
            Log.d(TAG, "generarToken/token_"+token);
            Log.d(TAG, "generarToken/idPeriodo"+ String.valueOf(idPeriodo));
            cargarPreguntasInnova();
        }
    }

    private void cargarPreguntasInnova() {
        if (ConexionBroadcastReceiver.isConnected()) {
            if (!(InnovaActivity.this).isFinishing()) { showDialog(pDialog); }
            JsonObjectRequest request = new JsonObjectRequest(
                    Constantes.URL_GET_PREGUNTAS,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                MetodoServices.eliminarPreguntasInnova();
                                Realm realm = Realm.getDefaultInstance();
                                JSONObject data = response.getJSONObject("data");
                                JSONArray array = data.getJSONArray("dataSubcategorias");
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject object1 = array.getJSONObject(i);
                                    int tipo_categoria = object1.getInt("idCategoria");
                                    if (tipo_categoria == 2) {
                                        realm.beginTransaction();
                                        Innova innova = realm.createObject(Innova.class);
                                        innova.setId_inn((long) object1.getLong("idSubCategoria"));
                                        innova.setIcono_inn(object1.getString("imagen_subc"));
                                        innova.setCategoria_inn(object1.getString("nombre_subc"));
                                        innova.setEstado_inn(false);
                                        innova.setEstado_orden_inn(false);
                                        innova.setId_periodo_inn(object1.getInt("idPeriodo"));
                                        realm.copyToRealm(innova);
                                        realm.commitTransaction();
                                        realm.close();
                                        Log.d(TAG, innova.toString());
                                    }
                                }

                                Innova innova_ = realm.where(Innova.class).findFirst();
                                realm.beginTransaction();
                                Innova innova1 = new Innova();
                                innova1.setId_inn(innova_.getId_inn());
                                innova1.setIcono_inn(innova_.getIcono_inn());
                                innova1.setCategoria_inn(innova_.getCategoria_inn());
                                innova1.setEstado_inn(true);
                                innova1.setEstado_orden_inn(true);
                                innova1.setId_periodo_inn(innova_.getId_periodo_inn());
                                realm.copyToRealmOrUpdate(innova1);
                                realm.commitTransaction();
                                realm.close();

                                Realm realm2 = Realm.getDefaultInstance();
                                JSONArray array2 = data.getJSONArray("dataPreguntas");
                                for (int i = 0; i < array2.length(); i++) {
                                    JSONObject object2 = array2.getJSONObject(i);
                                    int id_pregunta = object2.getInt("idPregunta");
                                    String pregunta = object2.getString("pregunta");
                                    String pregt_pa = object2.getString("rp4");
                                    boolean pregt_pa2 = (pregt_pa == null || pregt_pa.isEmpty() || pregt_pa.equals("null"));
                                    long id_json = Long.parseLong(object2.getString("idSubCategoria"));
                                    Innova innova = realm2.where(Innova.class).equalTo(Innova.I_ID, id_json).findFirst();
                                    if (innova != null) {
                                        realm2.beginTransaction();
                                        PreguntaInnova pi = realm2.createObject(PreguntaInnova.class);
                                        pi.setId_pregunta_inn(PreguntaInnova.getUltimoId());
                                        pi.setId_pregunta_servidor_inn(id_pregunta);
                                        pi.setNum_pregunta_inn((int) innova.getId_inn());
                                        pi.setPregunta_inn(pregunta);
                                        pi.setValor_respuesta(pregt_pa2);
                                        pi.setValor_ace("");
                                        pi.setRespondido_inn(false);
                                        pi.setId_boton_inn(-1);
                                        innova.getPreguntas().add(pi);
                                        realm2.copyToRealm(pi);
                                        realm2.commitTransaction();
                                        realm2.close();
                                        Log.d(TAG, pi.toString());
                                    }
                                }
                                Preferencia.iniciarPreferenciaInnova(-1, -1, 1, InnovaActivity.this);
                                startActivity(new Intent(InnovaActivity.this, InnovaActivity.class));
                                finish();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            hidepDialog(pDialog);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (error != null) { VolleyLog.d(error.toString()); }
                            hidepDialog(pDialog);
                            if (ConexionBroadcastReceiver.isConnected()) { cargarPreguntasInnova(); }
                        }
                    }
            );
            Configuracion.getInstance().addToRequestQueue(request, TAG);
        } else { showSnack(layout); }
    }

    private void generarToken(final String fecha, final int id_categoria, final String token, final int idPeriodo) {
        if (ConexionBroadcastReceiver.isConnected()) {
            if (!(InnovaActivity.this).isFinishing()){ showDialog(pDialog); }
            StringRequest request = new StringRequest(
                    Request.Method.POST,
                    Constantes.URL_INSERT_GET_ID,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject object = new JSONObject(response);
                                int id_test = object.getInt("id");
                                //TODO: CUANDO RECIBO EL ID DEL TEST, ACTIVO UN REGISTRO(TEST) REALM PARA USARLO TEST2
                                Test.activarTests(2, id_test);
                                Log.d(TAG, String.valueOf(object));
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.d(TAG, e.toString());
                            }
                            hidepDialog(pDialog);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            hidepDialog(pDialog);
                            if (error != null) {
                                VolleyLog.d(error.toString());
                            }
                            if (ConexionBroadcastReceiver.isConnected()) {
                                generarToken(fecha, id_categoria, token, idPeriodo);
                            } else if (!ConexionBroadcastReceiver.isConnected()) {
                                Toast.makeText(getApplicationContext(), "Verifique su conexión a internet.", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(InnovaActivity.this, PrincipalActivity.class));
                                finish();
                            }
                        }
                    }
            ) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("fecha", fecha);
                    params.put("idCategoria", String.valueOf(id_categoria));
                    params.put("token", token);
                    params.put("from", "android");
                    params.put("idPeriodo", String.valueOf(idPeriodo));
                    return params;
                }
            };
            Configuracion.getInstance().addToRequestQueue(request, TAG);
        }else { showSnack(layout); }
    }

    private void hacerTransicion() {
        Slide slide1 = new Slide();
        slide1.setDuration(1000);
        getWindow().setEnterTransition(slide1);

        Fade fade2 = new Fade();
        fade2.setDuration(1000);
        getWindow().setReturnTransition(fade2);
    }

    @Override
    protected void onResume() {
        super.onResume();
        iniciarLayout();
    }

    private void iniciarLayout() {
        realm = Realm.getDefaultInstance();
        listaInnova = realm.where(Innova.class).findAll();
        realmAdapter = new InnovaRealmAdapter(this, listaInnova, true, true);
        listaRecyclerInnova.setAdapter(realmAdapter);
        realmAdapter.notifyDataSetChanged();
        usarGlide(this, R.drawable.tarjeta_innova, cabeceraInnova);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home){ onBackPressed();}
        return super.onOptionsItemSelected(item);
    }

}
