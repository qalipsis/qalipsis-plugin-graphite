//package io.qalipsis.plugins.graphite.save
//
//import assertk.all
//import assertk.assertThat
//import assertk.assertions.hasSize
//import assertk.assertions.index
//import assertk.assertions.isEqualTo
//import assertk.assertions.isInstanceOf
//import assertk.assertions.isNotNull
//import assertk.assertions.key
//import com.mongodb.reactivestreams.client.MongoClient
//import com.mongodb.reactivestreams.client.MongoClients
//import io.micrometer.core.instrument.Counter
//import io.micrometer.core.instrument.MeterRegistry
//import io.micrometer.core.instrument.Tags
//import io.micrometer.core.instrument.Timer
//import io.mockk.confirmVerified
//import io.mockk.every
//import io.mockk.verify
//import io.qalipsis.api.context.StepStartStopContext
//import io.qalipsis.api.events.EventsLogger
//import io.qalipsis.api.sync.SuspendedCountLatch
//import io.qalipsis.plugins.graphite.GraphiteClient
//import io.qalipsis.plugins.mondodb.save.MongoDbSaveQueryClientImpl
//import io.qalipsis.plugins.mondodb.save.MongoDbSaveQueryMeters
//import io.qalipsis.plugins.mongodb.Constants.DOCKER_IMAGE
//import io.qalipsis.test.assertk.prop
//import io.qalipsis.test.coroutines.TestDispatcherProvider
//import io.qalipsis.test.mockk.WithMockk
//import io.qalipsis.test.mockk.relaxedMockk
//import org.bson.Document
//import org.junit.jupiter.api.AfterAll
//import org.junit.jupiter.api.BeforeAll
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.Timeout
//import org.junit.jupiter.api.assertThrows
//import org.reactivestreams.Subscriber
//import org.reactivestreams.Subscription
//import org.testcontainers.containers.MongoDBContainer
//import org.testcontainers.containers.wait.strategy.Wait
//import org.testcontainers.junit.jupiter.Container
//import org.testcontainers.junit.jupiter.Testcontainers
//import org.testcontainers.utility.DockerImageName
//import java.time.Duration
//import java.util.concurrent.TimeUnit
//import kotlin.math.pow
//
///**
// *
// * @author Palina Bril
// */
//@Testcontainers
//@WithMockk
//internal class GraphiteSaveStepIntegrationTest {
//
//    private lateinit var client: GraphiteClient
//
//    val testDispatcherProvider = TestDispatcherProvider()
//
//    @BeforeAll
//    fun init() {
//        client = MongoClients.create("mongodb://localhost:${mongodb.getMappedPort(27017)}/?streamType=netty")
//    }
//
//    @AfterAll
//    fun shutDown() {
//        client.close()
//    }
//
//    private val timeToResponse = relaxedMockk<Timer>()
//
//    private val recordsCount = relaxedMockk<Counter>()
//
//    private val failureCounter = relaxedMockk<Counter>()
//
//    private val eventsLogger = relaxedMockk<EventsLogger>()
//
//    @Test
//    @Timeout(10)
//    fun `should succeed when sending query with single results`() = testDispatcherProvider.run {
//        val metersTags = relaxedMockk<Tags>()
//        val meterRegistry = relaxedMockk<MeterRegistry> {
//            every { counter("mongodb-save-saving-records", refEq(metersTags)) } returns recordsCount
//            every { timer("mongodb-save-time-to-response", refEq(metersTags)) } returns timeToResponse
//        }
//        val startStopContext = relaxedMockk<StepStartStopContext> {
//            every { toMetersTags() } returns metersTags
//        }
//        val countLatch = SuspendedCountLatch(1)
//        val results = ArrayList<Document>()
//        val document = Document("key1", "val1")
//        val saveClient = MongoDbSaveQueryClientImpl(
//            ioCoroutineScope = this,
//            clientBuilder = { client },
//            meterRegistry = meterRegistry,
//            eventsLogger = eventsLogger
//        )
//        val tags: Map<String, String> = emptyMap()
//
//        saveClient.start(startStopContext)
//
//        val resultOfExecute = saveClient.execute("db1", "col1", listOf(document), tags)
//
//        assertThat(resultOfExecute).isInstanceOf(MongoDbSaveQueryMeters::class.java).all {
//            prop("savedRecords").isEqualTo(1)
//            prop("failedRecords").isEqualTo(0)
//            prop("failedRecords").isNotNull()
//        }
//
//        fetchResult(client, "db1", "col1", results, countLatch)
//        countLatch.await()
//        assertThat(results).all {
//            hasSize(1)
//            index(0).all {
//                key("key1").isEqualTo("val1")
//            }
//        }
//
//        verify {
//            eventsLogger.debug("mongodb.save.saving-records", 1, any(), tags = tags)
//            timeToResponse.record(more(0L), TimeUnit.NANOSECONDS)
//            recordsCount.increment(1.0)
//            eventsLogger.info("mongodb.save.time-to-response", any<Duration>(), any(), tags = tags)
//            eventsLogger.info("mongodb.save.saved-records", any<Array<*>>(), any(), tags = tags)
//        }
//        confirmVerified(timeToResponse, recordsCount, eventsLogger)
//    }
//
//    @Test
//    @Timeout(10)
//    fun `should throw an exception when sending invalid documents`(): Unit = testDispatcherProvider.run {
//        val metersTags = relaxedMockk<Tags>()
//        val meterRegistry = relaxedMockk<MeterRegistry> {
//            every { counter("mongodb-save-failures", refEq(metersTags)) } returns failureCounter
//        }
//        val startStopContext = relaxedMockk<StepStartStopContext> {
//            every { toMetersTags() } returns metersTags
//        }
//
//        val saveClient = MongoDbSaveQueryClientImpl(
//            ioCoroutineScope = this,
//            clientBuilder = { client },
//            meterRegistry = meterRegistry,
//            eventsLogger = eventsLogger
//        )
//        val tags: Map<String, String> = emptyMap()
//        saveClient.start(startStopContext)
//
//        assertThrows<Exception> {
//            saveClient.execute(
//                "db2",
//                "col2",
//                listOf(Document("key1", Duration.ZERO)), // Duration is not supported.
//                tags
//            )
//        }
//        verify {
//            failureCounter.increment(1.0)
//        }
//        confirmVerified(failureCounter)
//    }
//
//    private fun fetchResult(
//        client: MongoClient, database: String, collection: String, results: ArrayList<Document>,
//        countLatch: SuspendedCountLatch
//    ) {
//        client.run {
//            getDatabase(database)
//                .getCollection(collection)
//                .find(Document())
//                .subscribe(
//                    object : Subscriber<Document> {
//                        override fun onSubscribe(s: Subscription) {
//                            s.request(Long.MAX_VALUE)
//                        }
//
//                        override fun onNext(document: Document) {
//                            results.add(document)
//                            countLatch.blockingDecrement()
//                        }
//
//                        override fun onError(error: Throwable) {}
//
//                        override fun onComplete() {}
//                    }
//                )
//        }
//    }
//
//    companion object {
//
//        @Container
//        @JvmStatic
//        val mongodb = MongoDBContainer(DockerImageName.parse(DOCKER_IMAGE))
//            .apply {
//                waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)))
//                withCreateContainerCmdModifier { cmd ->
//                    cmd.hostConfig!!.withMemory(512 * 1024.0.pow(2).toLong()).withCpuCount(2)
//                }
//            }
//    }
//}