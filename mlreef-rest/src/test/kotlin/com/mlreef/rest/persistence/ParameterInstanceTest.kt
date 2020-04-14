package com.mlreef.rest.persistence

import com.mlreef.rest.DataOperation
import com.mlreef.rest.DataProcessorInstance
import com.mlreef.rest.DataType
import com.mlreef.rest.ParameterInstance
import com.mlreef.rest.ParameterInstanceRepository
import com.mlreef.rest.ParameterType
import com.mlreef.rest.Person
import com.mlreef.rest.ProcessorParameter
import com.mlreef.rest.ProcessorParameterRepository
import com.mlreef.rest.VisibilityScope
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID
import java.util.UUID.randomUUID
import javax.transaction.Transactional

class ParameterInstanceTest : AbstractRepositoryTest() {
    @Autowired
    private lateinit var repository: ParameterInstanceRepository

    @Autowired
    private lateinit var processorParameterRepository: ProcessorParameterRepository


    private fun createEntity(): Pair<UUID, ParameterInstance> {
        val id = randomUUID()
        val codeProjectId = randomUUID()
        val author = Person(randomUUID(), "slug", "name", 1L)
//        val codeProject = CodeProject(id = codeProjectId, slug = "code-project-augment", ownerId = author.id, url = "url")
        val dataProcessor = DataOperation(
            id = randomUUID(), slug = "commons-random-crop", name = "Random crop",
            command = "random_crop", inputDataType = DataType.IMAGE, outputDataType = DataType.IMAGE,
            visibilityScope = VisibilityScope.PUBLIC, author = author,
            description = "description",
            codeProjectId = codeProjectId)

        val processorParameter = ProcessorParameter(randomUUID(), dataProcessor.id, "height", ParameterType.INTEGER, 1, "")
        val dataProcessorInstance = DataProcessorInstance(id = randomUUID(), dataProcessor = dataProcessor)
        val entity = ParameterInstance(
            id = id, processorParameter = processorParameter,
            dataProcessorInstanceId = dataProcessorInstance.id, value = "value")

        processorParameterRepository.save(processorParameter)
        return Pair(id, entity)
    }

    @BeforeEach
    fun prepare() {
        truncateDbTables(listOf("account", "account_token"), cascade = true)
    }

    @Transactional
    @Test
    fun `find works`() {
        val (id, entity) = createEntity()

        Assertions.assertThat(repository.findByIdOrNull(id)).isNull()
        repository.save(entity)
        Assertions.assertThat(repository.findByIdOrNull(id)).isNotNull()
    }

    @Transactional
    @Test
    fun `save works`() {
        val (id, entity) = createEntity()
        Assertions.assertThat(repository.findByIdOrNull(id)).isNull()
        val saved = repository.save(entity)
        Assertions.assertThat(saved).isNotNull()
        checkAfterCreated(saved)
        Assertions.assertThat(repository.findByIdOrNull(id)).isNotNull()
    }

    @Transactional
    @Test
    fun `update works`() {
        val (_, entity) = createEntity()
        val saved = repository.save(entity)
        val newValue = "newname"
        val copy = saved.copy(value = newValue)
        val updated = repository.save(copy)
        Assertions.assertThat(updated).isNotNull()
        checkAfterUpdated(updated)
        Assertions.assertThat(updated.value).isEqualTo(newValue)
    }

    @Transactional
    @Test
    fun `delete works`() {
        val (_, entity) = createEntity()
        val saved = repository.save(entity)
        repository.delete(saved)
        Assertions.assertThat(saved).isNotNull()
        checkAfterCreated(saved)
    }
}
