package controllers

import javax.inject._
import play.api.libs.json.{JsArray, Json}
import play.api.mvc._
import utilities.{Constants, Dataset, Pre, Stages, Statistics}

@Singleton
class MainController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
	val spark = Pre.spark(Constants.APP_NAME, Constants.MASTER)
	val steam = Pre.dataframe(spark, Constants.ROOT + Constants.STEAM_FETCHED_DATA)
	val kaggle = Pre.dataframe(spark, Constants.ROOT + Constants.KAGGLE_DATA)
	val classified_kaggle = Pre.doCluster(kaggle)

	def index: Action[AnyContent] = Action {
		val string =
			"""
			  |IntelliDota - Classification and Clustering
			  |Universiteti i Prishtines @2019
			  |
			  |
			  |
			  |
			  |
			  |Funksioni:       getColumns(kind: String)
			  |URL:             /getColumns?kind=steam
			  |Përshkrimi:      Nuk ka ndonjë përdorim specifik përveç se na ndihmon që të marrim kolonat e data seteve në mënyrë që të mos jenë të koduara në mënyrë të vrazhdë (ang. hard coded).
			  |
			  |
			  |
			  |
			  |
			  |Funksioni:       getSample(kind: String, percentage: Double)
			  |URL:             /getSample?kind=steam&percentage=10
			  |Përshkrimi:      Përdoret për të vizualizuar të dhënat, konkretisht shfaqen kolonat me rreshtat përkatës, si një lloj tabele.
			  |Shembull:
			  |--------------------------------------------------------------------------
			  ||    kolona1 |   kolona2 |   kolona3 |   kolona4 |   kolona5 |   kolona6 |
			  |--------------------------------------------------------------------------
			  ||    vlera10 |   vlera11 |   vlera12 |   vlera13 |   vlera14 |   vlera15 |
			  |--------------------------------------------------------------------------
			  ||    vlera20 |   vlera21 |   vlera22 |   vlera23 |   vlera24 |   vlera25 |
			  |--------------------------------------------------------------------------
			  |
			  |
			  |
			  |
			  |
			  |Funksioni:       getStages(kind: String)
			  |URL:             /getStages?kind=kaggle
			  |Përshkrimi:      Përdoret për të shfaqur fazat nëpër të cilat kaluar algoritmi, si një gyp i vazhdueshëm.
			  |Shembull:
			  |Për thirrjen e mësipërme, do të donim të merrnim diqka si:
			  |   _________________________o________________________________________________________o_________________________
			  |                         vecAssembler                                                 kmeans
			  |                         inputCols:   [Ljava.lang...]                                 k:         6
			  |                         outputCols:  features                                        maxIteR:   25
			  |
			  |Për thirrjen /getStages?kind=steam do të donim të merrnim diqka si:
			  |   _________________________o________________________________________________________o________________________________________________________o_________________________
			  |                         vecAssembler                                            stdScal                                                     rfc
			  |                         inputCols:  [Ljava.lang...]                             inputCol:   non-scaled                                  featuresCol:    features
			  |                         outputCol:  non-scaled                                  outputCol:  features                                    labelCol:       label
			  |                                                                                 withMean:   true                                        numTrees:       10
			  |                                                                                 withStd:    true
			  |Vërejtje: emri i klasës, për shembull 'vecAssembler' apo 'stdScal' apo 'kmeans' ndahet në vijën e poshtme (splits on underline) dhe merret vetëm pjesa e parë e emrit. Pjesa e dytë nuk është e nevojshme
			  |pasi paraqet vetëm një ID të klasës përkatëse. Po ashtu, asnjë vlerë nuk është hard coded, të gjitha duhet të merren nga përgjigjja e kërkesës.
			  |
			  |
			  |
			  |
			  |
			  |Funksioni:       getCorrelationMatrix(kind: String)
			  |URL:             /getCorrelationMatrix?kind=kaggle
			  |Përshkrimi:      Rikthen një matricë A x A ku A - numri i kolonave të data setit përkatës, në rastin tonë kaggle, duhet të kombinohet me 'getColumns' të data setit përkatës.
			  |Shembull:
			  |Për thirrjen e mësipërme, matrica do të duket kështu:
			  |----------------------------------------------------------------------------------------------------------
			  ||                    |    gold_per_min    |   level      |   leaver_status   |   xp_per_min  |   ...     |
			  |----------------------------------------------------------------------------------------------------------
			  ||    gold_per_min    |   1.0              |   0.8652708  |   0.7978521       |   0.50636069  |   ...     |
			  ||    level           |   0.8652708135101  |   1          |   0.9255410       |   0.65170968  |   ...     |
			  ||    leaver_status   |   ...              |   ...        |   1               |   ...         |   ...     |
			  ||    xp_per_min      |   ...              |   ...        |   ...             |   ...         |   ...     |
			  |----------------------------------------------------------------------------------------------------------
			  |
			  |
			  |
			  |
			  |
			  |Funksioni:       getStats(kind: String)
			  |URL:             /getStats?kind=steam
			  |Përshkrimi:      Rikthen rreshtat dhe kolonat e data setit përkatës, janë përdorur për raport por këto informata janë të dobishme edhe për vizualizim.
			  |
			  |
			  |
			  |
			  |
			  |Funksioni:       getSchema(kind: String)
			  |URL:             /getSchema?kind=steam
			  |Përshkrimi:      Rikthen skemën e data setit përkatës, e dobishme për raport dhe vizualizim.
			  |Shembull:
			  |Një tabelë e thjeshtë ku paraqiten çiftet, pra:
			  |----------------------------------
			  ||    kolona      |   tipi        |
			  |----------------------------------
			  |     xp_per_min  |   double      |
			  |----------------------------------
			  |     gold_per_min|   double      |
			  |----------------------------------
			  |
			  |
			  |
			  |
			  |
			  |Funksioni:           getGroupAndCount(kind: String, attribute: String, partitions: Option[Int])
			  |URL:                 /getGroupAndCount?kind=kaggle&attribute=gold&partitions=4
			  |Përshkrimi:          Rikthen grupimin varësisht particioneve, për shembull kthen një grafik ku në X është një atribut dhe në Y numri i njehësuar i grupimeve.
			  |Shembull:
			  |Për thirrjen e mësipërme, rezultati do të dukej kështu:
			  |      51 |           |
			  |      40 |  |        |
			  |      14 |  |        |       |
			  |         |  |        |       |
			  |      5  |  |        |       |       |
			  |         ---------------------------------------
			  |             0       1       2       3
			  |         (1366-1716)     (2066-2416)
			  |                 (1716-2066)     (2416-2769)
			  |
			  |
			  |
			  |
			  |
			  |Funksioni:           getClusterCount
			  |URL:                 /getClusterCount
			  |Përshkrimi:          Kthen grupimet dhe njehësimet
			  |Shembull:
			  |Për thirrjen e mësipërme, rezultati do të dukej kështu:
			  |      36 |  |
			  |      30 |  |                        |
			  |      21 |  |                        |       |
			  |      15 |  |                |       |       |
			  |      6  |  |        |       |       |       |
			  |      2  |  |        |       |       |       |       |
			  |         --------------------------------------------
			  |             0       1       2      3        4       5
			  |
			  |
			  |
			  |
			  |
			  |Funksioni:           getClusterStats
			  |URL:                 /getClusterStats
			  |Përshkrimi:          Kthen kllasterimin që mund të vizualizohet si hapi më sipër.
			  |
			  |
			  |
			  |
			  |postPredict
			  |{
			  |	"gold_per_min": 2585,
			  |	"level": 139,
			  |	"leaver_status": 0,
			  |	"xp_per_min": 0,
			  |	"radiant_score": 34,
			  |	"gold_spent": 0,
			  |	"deaths": 0,
			  |	"denies": 21,
			  |	"hero_damage": 0,
			  |	"tower_damage": 2732,
			  |	"last_hits": 0,
			  |	"hero_healing": 0,
			  |	"duration": 0
			  |}
			  |
			  |
			  |
			  |
			  |
			  |postCluster
			  |{
			  |	"gold": 2000.0,
			  |	"gold_per_min": 5113.0,
			  |	"xp_per_min": 1231.0,
			  |	"kills": 21.2,
			  |	"deaths": 3.3,
			  |	"assists": 10.0,
			  |	"denies": 34.3,
			  |	"last_hits": 411.1,
			  |	"hero_damage": 56000.2,
			  |	"hero_healing": 0.0,
			  |	"tower_damage": 22000.0,
			  |	"level": 25
			  |}
			  |""".stripMargin

		Ok(string)
	}

	def getColumns(kind: String): Action[AnyContent] = Action {
		kind match {
			case Constants.STEAM => Ok(Dataset.getSteamColumns(steam))
			case Constants.KAGGLE => Ok(Dataset.getKaggleColumns(kaggle))
		}
	}
	def getSample(kind: String, percentage: Double): Action[AnyContent] = Action {
		kind match {
			case Constants.STEAM => Ok(Dataset.getSample(steam, percentage / 100))
			case Constants.KAGGLE => Ok(Dataset.getSample(kaggle, percentage / 100))
		}
	}
	def getStages(kind: String): Action[AnyContent] = Action {
		kind match {
			case Constants.STEAM => Ok(Stages.getStages(Constants.ROOT + Constants.CLASSIFIED_MODEL))
			case Constants.KAGGLE => Ok(Stages.getStages(Constants.ROOT + Constants.CLUSTERED_MODEL))
		}
	}
	def getCorrelationMatrix(kind: String): Action[AnyContent] = Action {
		kind match {
			case Constants.STEAM => Ok(Dataset.getCorrelationMatrix(steam))
			case Constants.KAGGLE => Ok(Dataset.getCorrelationMatrix(kaggle))
		}
	}
	def getStats(kind: String): Action[AnyContent] = Action {
		kind match {
			case Constants.STEAM => Ok(Dataset.getStats(Constants.STEAM, steam))
			case Constants.KAGGLE => Ok(Dataset.getStats(Constants.KAGGLE, kaggle))
		}
	}
	def getSchema(kind: String): Action[AnyContent] = Action {
		kind match {
			case Constants.STEAM => Ok(Dataset.getSchema(steam))
			case Constants.KAGGLE => Ok(Dataset.getSchema(kaggle))
		}
	}
	def getDoubleGroup(kind: String, col1: String, col2: String): Action[AnyContent] = Action {
		kind match {
			case Constants.STEAM => Ok(Dataset.getDoubleGroup(steam, col1, col2))
			case Constants.KAGGLE => Ok(Dataset.getDoubleGroup(kaggle, col1, col2))
		}
	}
	def getGroupAndCount(kind: String, attribute: String, partitions: Option[Int]): Action[AnyContent] = Action {
		kind match {
			case Constants.STEAM => {
				attribute match {
					case Constants.LEAVER_STATUS | Constants.RADIANT_WIN => Ok(Statistics.getBinary(steam, attribute))
					case _                                  => Ok(Statistics.get(spark, steam, attribute, partitions.get))
				}
			}
			case Constants.KAGGLE => Ok(Statistics.get(spark, kaggle, attribute, partitions.get))
		}
	}

	def postPredict: Action[AnyContent] = Action { request =>

		val attributes = request.body.asJson.get.toString
			.replace("{", "").replace("}", "")
    		.split(",").map(row => row.split(":")(1).toInt).toSeq

		val result = Dataset.predict(spark, steam, attributes)

		Ok(result)
	}

	def postCluster: Action[AnyContent] = Action { request =>

		val attributes = request.body.asJson.get.toString
			.replace("{", "").replace("}", "")
			.split(",").map(row => row.split(":")(1).toDouble).toSeq

		val result = Dataset.cluster(spark, kaggle, attributes)

		Ok(result)
	}
	def getClusterCount: Action[AnyContent] = Action {
		val result = Statistics.getBinary(classified_kaggle, Constants.PREDICTION)

		Ok(result)
	}
	def getClusterStats: Action[AnyContent] = Action {
		val result = Dataset.getClusterStats(classified_kaggle)

		Ok(result)
	}


}
