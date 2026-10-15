package org.toyotacustom.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.toyotacustom.core.Obd

class MainActivity:ComponentActivity() {
 override fun onCreate(savedInstanceState:Bundle?) {super.onCreate(savedInstanceState);setContent{ToyotaApp()}}
}

@Composable fun ToyotaApp(vm:VehicleViewModel=viewModel()) {
 val s by vm.state.collectAsStateWithLifecycle()
 val history by vm.history.collectAsStateWithLifecycle()
 var tab by remember{mutableIntStateOf(0)}
 var theme by remember{mutableIntStateOf(0)}
 var developer by remember{mutableStateOf(false)}
 var clearDialog by remember{mutableStateOf(false)}
 var customDialog by remember{mutableStateOf<String?>(null)}
 var exportText by remember{mutableStateOf("")}
 val ctx=androidx.compose.ui.platform.LocalContext.current
 val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) {uri->
  if(uri!=null) runCatching {ctx.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use{it.write(exportText)}}
 }
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {r->if(r.values.all{it})vm.scan()}
 fun scan(){ permission.launch(if(Build.VERSION.SDK_INT>=31)arrayOf(Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)) }
 val dark=theme==2 || (theme==0 && isSystemInDarkTheme())
 MaterialTheme(colorScheme=if(dark)darkColorScheme(primary=Color(0xFFFFB4AB))else lightColorScheme(primary=Color(0xFFAD2929))) {
 Surface(Modifier.fillMaxSize()) {
 Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
  Column(Modifier.padding(horizontal=20.dp,vertical=12.dp)) {
   Text("TOYOTA CUSTOM",style=MaterialTheme.typography.headlineSmall)
   Text(if(s.simulation)"SIMULATION • données fictives" else "Diagnostic local • sans abonnement",color=MaterialTheme.colorScheme.primary)
   Text(s.status,style=MaterialTheme.typography.bodyMedium)
   if(s.busy)LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=8.dp))
  }
  ScrollableTabRow(selectedTabIndex=tab) {
   listOf("Accueil","Diagnostic","Personnalisation","Hybride","Historique","Paramètres").forEachIndexed {i,label->Tab(selected=tab==i,onClick={tab=i},text={Text(label)})}
  }
  Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   when(tab) {
    0->{
     InfoCard("Véhicule",s.vin?.let{"VIN : $it\n${if(Obd.toyota(it))"Constructeur Toyota identifié par WMI" else "Constructeur non confirmé"}\nModèle, année et équipement non déterminés"}?:"Aucun véhicule identifié")
     InfoCard("Profil prioritaire","Corolla 1.8 Hybrid • Europe • 2019\nProfil demandé ; il ne constitue pas une identification du véhicule connecté.")
     InfoCard("Boîtier", "${s.adapter}\n${s.protocol}\n${s.supported.size} PID standard annoncés")
     Button(onClick={tab=5},modifier=Modifier.fillMaxWidth()){Text("Configurer la connexion NexLink")}
     OutlinedButton(onClick={vm.connect(simulate=true)},enabled=!s.busy,modifier=Modifier.fillMaxWidth()){Text("Ouvrir la simulation")}
     if(s.connected)OutlinedButton(onClick={vm.disconnect()}){Text("Déconnecter")}
    }
    1->{
     val enabled=s.connected&&!s.busy&&(s.simulation||Obd.toyota(s.vin))
     InfoCard("Diagnostic OBD standard","Lecture des défauts antipollution mémorisés, en attente et permanents. Le balayage des calculateurs Toyota n’est pas encore implémenté.")
     Button(onClick={vm.diagnostic()},enabled=enabled){Text("Lire les codes défaut")}
     InfoCard("Résultat",s.codes?.let{if(it.isEmpty())"Aucun code retourné par les modes ayant répondu. Consultez les journaux pour les modes indisponibles."else it.joinToString("\n")}?:"Aucune lecture effectuée")
     OutlinedButton(onClick={clearDialog=true},enabled=enabled&&s.codes!=null){Text("Effacer les défauts standard…")}
     Button(onClick={vm.refresh()},enabled=enabled){Text("Actualiser les mesures")}
     var polling by remember{mutableStateOf(false)}
     Row {Checkbox(polling,{polling=it;vm.polling(it)},enabled=enabled);Text("Actualisation toutes les 3 secondes")}
     s.values.filterKeys{it!=91}.forEach{(pid,v)->InfoCard(pidName(pid),"%.1f %s".format(v,pidUnit(pid)))}
     if(s.values.isEmpty())Text("Aucune mesure lue. Seuls les PID annoncés par le véhicule sont interrogés.")
     Text("Valeurs ponctuelles : vérifiez l’heure de la dernière actualisation affichée en haut.")
    }
    2->{
     InfoCard("Catalogue de recherche","Les familles ci-dessous ont été repérées dans les analyses précédentes. Calculateur, valeurs et commandes restent à établir pour votre véhicule. Aucune lecture ou écriture Toyota n’est activée.")
     if(s.simulation)InfoCard("Banc de simulation","Le réglage fictif ci-dessous sert à tester la confirmation et l’historique. Il ne reproduit aucun protocole Toyota.")
     if(s.simulation) {
      InfoCard("Sensibilité des phares — fictive",s.custom["sim_lights"]?:"Normale (fictive)")
      Button(onClick={customDialog="Élevée (fictive)"}){Text("Simuler une modification…")}
     }
     vm.definitions.groupBy{it.category}.forEach{(category,items)->
      Text(category,style=MaterialTheme.typography.titleLarge)
      items.forEach{d->InfoCard(d.name,"${d.description}\nÀ vérifier • Compatibilité inconnue\nLecture : indisponible • Écriture : désactivée\n${d.source}")}
     }
    }
    3->{
     InfoCard("Système hybride","Les identifiants Toyota de batterie, blocs, températures et moteurs électriques ne sont pas validés. Aucune donnée constructeur n’est inventée.")
     if(91 in s.supported){
      Button(onClick={vm.refresh()},enabled=s.connected&&!s.busy){Text("Lire le PID standard 015B")}
      InfoCard("Charge batterie — PID OBD générique",s.values[91]?.let{"%.1f %% — interprétation spécifique Toyota à confirmer".format(it)}?:"Annoncé par le véhicule ; pas encore lu")
     }else Text("PID générique 015B non annoncé. Données hybrides constructeur non disponibles dans cette version.")
    }
    4->{
     InfoCard("Historique local","Chaque entrée distingue les opérations simulées. Les sauvegardes de réglages réels seront disponibles après validation des commandes Toyota.")
     if(history.isEmpty())Text("Aucune opération enregistrée")
     history.forEach{h->
      InfoCard((if(h.simulated)"SIMULATION • "else "VÉHICULE • ")+h.parameter,"${java.time.Instant.ofEpochMilli(h.time)}\nAvant : ${h.before}\nAprès : ${h.after}\n${h.result}")
      if(h.simulated && h.parameter=="sim_lights")OutlinedButton(onClick={vm.simulateChange(h.parameter,h.before,true)},enabled=s.simulation&&s.connected){Text("Restaurer cette valeur en simulation")}
     }
    }
    5->{
     Text("Connexion Bluetooth",style=MaterialTheme.typography.titleLarge)
     Text("Appairez NEXAS dans Android (PIN indiqué par le fabricant : 1234), puis sélectionnez l’appareil. La sélection déclenche la connexion SPP ; 2 nouvelles tentatives sont faites en cas d’échec initial.")
     Button(onClick={scan()},enabled=!s.busy){Text("Rechercher et autoriser le Bluetooth")}
     OutlinedButton(onClick={ctx.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))}){Text("Ouvrir les paramètres Bluetooth Android")}
     s.devices.forEach{d->
      InfoCard(d.name,"${d.address}\n${if(d.bonded)"Appairé"else "Non appairé"} • ${if(d.ble)"Détecté en BLE"else "Classic / appareil associé"}")
      if(!d.bonded)OutlinedButton(onClick={vm.pair(d)},enabled=!s.busy){Text("Appairer dans Android")}
      Button(onClick={vm.connect(d)},enabled=!s.busy&&d.bonded){Text("Connecter via Bluetooth Classic SPP")}
     }
     Text("BLE : détection disponible. Transport ELM sur GATT non activé : UUID et protocole NexLink à vérifier. Aucun service série n’est présumé.")
     if(s.connected)OutlinedButton(onClick={vm.disconnect()}){Text("Déconnecter")}
     Text("Apparence",style=MaterialTheme.typography.titleLarge)
     Row{listOf("Système","Clair","Sombre").forEachIndexed{i,t->TextButton(onClick={theme=i}){Text(t)}}}
     Row{Switch(developer,{developer=it});Text("Outils de développement",Modifier.padding(8.dp))}
     if(developer){
      InfoCard("Protocole et capacités","${s.protocol}\nPID : ${s.supported.sorted().joinToString{"%02X".format(it)}}\nIdentifiants CAN et calculateurs : non capturés (en-têtes ELM désactivés).\nAucune console d’écriture libre.")
      Button(onClick={exportText=s.logs.joinToString("\n");exporter.launch("toyota-custom-journal.txt")}){Text("Exporter les journaux techniques")}
      Text("Les journaux peuvent contenir le VIN et des identifiants Bluetooth. Choisissez vous-même leur destinataire.")
      s.logs.takeLast(80).forEach{Text(it,style=MaterialTheme.typography.bodySmall)}
     }
     InfoCard("À propos","Toyota Custom 0.1.0 • code original sous licence MIT\nApplication indépendante, sans affiliation Toyota. Fonctionnement local sans autorisation Internet.")
    }
   }
  }
 }
 }
 if(clearDialog)AlertDialog(onDismissRequest={clearDialog=false},title={Text("Confirmer l’effacement")},text={Text("Véhicule immobilisé, frein de stationnement serré, mode READY désactivé et contact mis. L’effacement supprime des informations utiles au diagnostic et réinitialise les moniteurs antipollution. Les codes permanents peuvent rester. L’application relira vitesse et régime avant l’envoi. Confirmez que ces conditions sont remplies.")},confirmButton={TextButton(onClick={clearDialog=false;vm.clear(true)}){Text(if(s.simulation)"Confirmer en simulation"else "Confirmer l’effacement")}},dismissButton={TextButton(onClick={clearDialog=false}){Text("Annuler")}})
 customDialog?.let{value->AlertDialog(onDismissRequest={customDialog=null},title={Text("Confirmer la simulation")},text={Text("Sauvegarder la valeur fictive actuelle et appliquer $value dans le simulateur ? Aucun calculateur réel ne sera contacté.")},confirmButton={TextButton(onClick={vm.simulateChange("sim_lights",value);customDialog=null}){Text("Simuler")}},dismissButton={TextButton(onClick={customDialog=null}){Text("Annuler")}})}
 }
}
@Composable fun InfoCard(title:String,body:String) {
 Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(title,style=MaterialTheme.typography.titleMedium);Text(body)}}
}
fun pidName(pid:Int)=when(pid){4->"Charge moteur";5->"Liquide de refroidissement";12->"Régime moteur";13->"Vitesse véhicule";17->"Position du papillon";66->"Tension calculateur";else->"PID %02X".format(pid)}
fun pidUnit(pid:Int)=when(pid){4,17->"%";5->"°C";12->"tr/min";13->"km/h";66->"V";else->""}
