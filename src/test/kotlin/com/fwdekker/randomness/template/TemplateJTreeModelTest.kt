package com.fwdekker.randomness.template

import com.fwdekker.randomness.Scheme
import com.fwdekker.randomness.testhelpers.DummyScheme
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.beEmptyIntArray
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.shouldContainExactly
import com.fwdekker.randomness.testhelpers.shouldMatchBundle
import com.fwdekker.randomness.ui.SimpleTreeModelListener
import com.intellij.ui.RowsDnDSupport.RefinedDropSupport.Position
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.Row2
import io.kotest.data.Row3
import io.kotest.data.row
import io.kotest.datatest.withData
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener


/**
 * Unit tests for [TemplateJTreeModel].
 */
object TemplateJTreeModelTest : FunSpec({
    tags(Tags.PLAIN)


    lateinit var list: TemplateList
    lateinit var model: TemplateJTreeModel

    fun List<Scheme>.names() = this.map { it.name }


    beforeNonContainer {
        list = TemplateList(
            mutableListOf(
                Template(
                    "Template0", // row 0
                    mutableListOf(
                        DummyScheme("Scheme0"), // row 1
                        DummyScheme("Scheme1"), // row 2
                    ),
                ),
                Template(
                    "Template1", // row 3
                    mutableListOf(DummyScheme("Scheme2")), // row 4
                ),
                Template(
                    "Template2", // row 5
                    mutableListOf(),
                ),
            )
        )
        model = TemplateJTreeModel(list)
    }


    context("addRow") {
        test("throws an error") {
            shouldThrow<UnsupportedOperationException> { model.addRow() }
        }
    }

    context("removeRow") {
        test("throws an error") {
            shouldThrow<UnsupportedOperationException> { model.removeRow(4) }
        }
    }


    context("insertNode") {
        test("throws an error if the parent is not in the tree") {
            val parent = StateNode(Template())
            val child = StateNode(DummyScheme())

            shouldThrow<IllegalArgumentException> { model.insertNode(parent, child) }
                .message shouldMatchBundle "template_list.error.node_not_in_tree"
        }

        test("throws an error if the child is not a valid child for the parent") {
            val parent = model.root
            val child = StateNode(DummyScheme())

            shouldThrow<IllegalArgumentException> { model.insertNode(parent, child) }
                .message shouldMatchBundle "template_list.error.wrong_child_type"
        }

        test("throws an error when a node is inserted at a negative index") {
            shouldThrow<IndexOutOfBoundsException> { model.insertNode(model.root, StateNode(Template()), index = -2) }
        }

        test("throws an error if a node is inserted at a too-high index") {
            shouldThrow<IndexOutOfBoundsException> { model.insertNode(model.root, StateNode(Template()), index = 14) }
        }

        test("throws an error if a node with the same uuid already exists in the parent") {
            shouldThrow<IllegalArgumentException> { model.insertNode(model.root, model.root.children[0]) }
                .message shouldMatchBundle "template_list.error.duplicate_uuid"
        }


        test("inserts a node at a specific index") {
            val node = StateNode(Template("Template3"))

            model.insertNode(model.root, node, index = 1)

            model.root.children shouldHaveSize 4
            model.root.children[1] shouldBe node
            list.templates.names() shouldContainExactly listOf("Template0", "Template3", "Template1", "Template2")
        }

        test("inserts a node at the last index of the parent") {
            val node = StateNode(Template("Template3"))

            model.insertNode(model.root, node, index = 3)

            model.root.children shouldHaveSize 4
            model.root.children[3] shouldBe node
            list.templates.names() shouldContainExactly listOf("Template0", "Template1", "Template2", "Template3")
        }


        test("replaces uuids of schemes with the same uuid in other templates") {
            val target = list.templates[0].schemes[0]
            val targetUuid = target.uuid

            model.insertNode(
                model.root,
                StateNode(Template(schemes = mutableListOf(target.deepCopy(retainUuid = true))))
            )

            list.templates[0].schemes[0].uuid shouldNotBe targetUuid
            list.templates.last().schemes[0].uuid shouldBe targetUuid
        }


        test("informs listeners of the insertion") {
            var lastEvent: TreeModelEvent? = null
            model.addTreeModelListener(SimpleTreeModelListener { lastEvent = it })

            val scheme = StateNode(DummyScheme())
            model.insertNode(model.root.children[0], scheme)

            lastEvent!!.treePath.lastPathComponent shouldBe model.root.children[0]
            lastEvent!!.childIndices shouldContainExactly arrayOf(2)
            lastEvent!!.children shouldContainExactly arrayOf(scheme)
        }

        test("informs listeners of a node insertion if the root's not-only child was inserted") {
            var lastEvent: TreeModelEvent? = null
            model.addTreeModelListener(SimpleTreeModelListener { lastEvent = it })

            val node = StateNode(Template("Template3"))
            model.insertNode(model.root, node, index = 1)

            lastEvent!!.treePath.lastPathComponent shouldBe model.root
            lastEvent!!.childIndices shouldContainExactly arrayOf(1)
            lastEvent!!.children shouldContainExactly arrayOf(node)
        }

        test("informs listeners of a structure change if the root's now-only child was inserted") {
            list.templates.clear()
            model.fireNodeStructureChanged()

            var lastEvent: TreeModelEvent? = null
            model.addTreeModelListener(SimpleTreeModelListener { lastEvent = it })

            val node = StateNode(Template("Template3"))
            model.insertNode(model.root, node)

            lastEvent!!.treePath.lastPathComponent shouldBe model.root
            lastEvent!!.childIndices should beEmptyIntArray()
            lastEvent!!.children shouldBe null
        }
    }

    context("insertNodeAfter") {
        test("throws an error if the parent is not in the tree") {
            val template = Template(schemes = mutableListOf(DummyScheme("after")))

            val parent = StateNode(template)
            val after = StateNode(template.schemes[0])
            val child = StateNode(DummyScheme("Child"))

            shouldThrow<IllegalArgumentException> { model.insertNodeAfter(parent, after, child) }
                .message shouldMatchBundle "template_list.error.node_not_in_tree"
        }

        test("throws an error if the parent cannot have children") {
            val parent = StateNode(list.templates[0].schemes[0])
            val after = StateNode(DummyScheme())
            val child = StateNode(DummyScheme())

            shouldThrow<IllegalStateException> { model.insertNodeAfter(parent, after, child) }
                .message shouldMatchBundle "template_list.error.infertile_parent"
        }

        test("throws an error if the node to insert after is not in the parent") {
            val parent = StateNode(list.templates[1])
            val after = StateNode(list.templates[0].schemes[0])
            val child = StateNode(DummyScheme())

            shouldThrow<IllegalArgumentException> { model.insertNodeAfter(parent, after, child) }
                .message shouldMatchBundle "template_list.error.wrong_parent"
        }


        test("inserts the node after the given node") {
            val parent = StateNode(list.templates[0])
            val after = StateNode(list.templates[0].schemes[1])
            val child = StateNode(DummyScheme("Child"))

            model.insertNodeAfter(parent, after, child)

            parent.children[2] shouldBe child
        }
    }

    context("removeNode") {
        test("throws an error if the given node is not contained in the model") {
            shouldThrow<IllegalArgumentException> { model.removeNode(StateNode(DummyScheme())) }
                .message shouldMatchBundle "template_list.error.node_not_in_tree"
        }

        test("throws an error if the given node is the root of the model") {
            shouldThrow<IllegalArgumentException> { model.removeNode(model.root) }
                .message shouldMatchBundle "template_list.error.cannot_remove_root"
        }


        test("removes a node from the model and the underlying list") {
            val node = model.root.children[1]

            model.removeNode(node)

            model.root.children shouldHaveSize 2
            model.root.children shouldNotContain node
            list.templates shouldHaveSize 2
            list.templates shouldNotContain node.state
        }

        test("informs listeners of the removal") {
            val node = model.root.children[2]

            var lastEvent: TreeModelEvent? = null
            model.addTreeModelListener(SimpleTreeModelListener { lastEvent = it })
            model.removeNode(node)

            lastEvent!!.treePath.lastPathComponent shouldBe model.root
            lastEvent!!.childIndices shouldContainExactly arrayOf(2)
            lastEvent!!.children shouldContainExactly arrayOf(node)
        }
    }

    context("exchangeRows") {
        test("throws an error") {
            shouldThrow<UnsupportedOperationException> { model.exchangeRows(1, 2) }
        }
    }

    context("canExchangeRows") {
        test("throws an error") {
            shouldThrow<UnsupportedOperationException> { model.canExchangeRows(1, 2) }
        }
    }

    context("canMoveRow") {
        beforeNonContainer {
            model.insertNode(StateNode(list.templates[2]), StateNode(DummyScheme("Scheme3"))) // row 6
        }


        withData(
            mapOf(
                "cannot move to itself" to row(3, 3, Position.BELOW, false),
                "cannot move from invalid index" to row(-2, 2, Position.ABOVE, false),
                "cannot move to invalid index" to row(1, -4, Position.BELOW, false),
                "cannot move template into scheme" to row(3, 1, Position.INTO, false),
                "cannot move template into template" to row(0, 5, Position.INTO, false),
                "cannot move template below template" to row(5, 0, Position.BELOW, false),
                "cannot move template below scheme" to row(0, 4, Position.BELOW, false),
                "cannot move template above next template" to row(0, 3, Position.ABOVE, false),
                "cannot move template above scheme" to row(3, 2, Position.ABOVE, false),
                "can move middle template above first template" to row(3, 0, Position.ABOVE, true),
                "can move middle template above last template" to row(0, 5, Position.ABOVE, true),
                "can move middle template below last template" to row(3, 5, Position.BELOW, true),
                "cannot move second-to-last template above last template" to row(3, 5, Position.ABOVE, false),
                "cannot move last template below last scheme" to row(5, 6, Position.BELOW, false),
                "cannot move scheme into scheme" to row(4, 2, Position.INTO, false),
                "cannot move scheme above first template" to row(1, 0, Position.ABOVE, false),
                "can move scheme into template" to row(6, 0, Position.INTO, true),
                "can move scheme above scheme" to row(2, 1, Position.ABOVE, true),
                "can move scheme below scheme" to row(1, 6, Position.BELOW, true),
            )
        ) { (fromIndex, toIndex, position, expected) ->
            model.canMoveRow(fromIndex, toIndex, position) shouldBe expected
        }


        test("can move middle template below last template if last template has no schemes") {
            model.insertNode(model.root, StateNode(Template("Template3"))) // row 7

            model.canMoveRow(3, 7, Position.BELOW) shouldBe true
        }
    }

    context("moveRow") {
        test("cannot swap out-of-bounds indices") {
            shouldThrow<IllegalArgumentException> { model.moveRow(3, 495, Position.BELOW) }
                .message shouldMatchBundle "template_list.error.cannot_move_row"
        }

        context("templates") {
            test("moves a template above the first template") {
                model.moveRow(5, 0, Position.ABOVE)

                list.templates.names() shouldContainExactly listOf("Template2", "Template0", "Template1")
            }

            test("moves a template above the last template") {
                model.moveRow(0, 5, Position.ABOVE)

                list.templates.names() shouldContainExactly listOf("Template1", "Template0", "Template2")
            }

            test("moves a template below the last template") {
                model.moveRow(3, 5, Position.BELOW)

                list.templates.names() shouldContainExactly listOf("Template0", "Template2", "Template1")
            }
        }

        context("schemes") {
            test("moves a scheme above the previous scheme under the same parent") {
                model.moveRow(2, 1, Position.ABOVE)

                list.templates[0].schemes.names() shouldContainExactly listOf("Scheme1", "Scheme0")
            }

            test("moves a scheme below the next scheme under the same parent") {
                model.moveRow(1, 2, Position.BELOW)

                list.templates[0].schemes.names() shouldContainExactly listOf("Scheme1", "Scheme0")
            }

            test("moves a scheme into its own parent") {
                model.moveRow(1, 0, Position.INTO)

                list.templates[0].schemes.names() shouldContainExactly listOf("Scheme1", "Scheme0")
            }

            test("moves a scheme into another template") {
                model.moveRow(1, 3, Position.INTO)

                list.templates[0].schemes.names() shouldContainExactly listOf("Scheme1")
                list.templates[1].schemes.names() shouldContainExactly listOf("Scheme2", "Scheme0")
            }

            test("moves a scheme above a scheme in another template") {
                model.moveRow(2, 4, Position.ABOVE)

                list.templates[0].schemes.names() shouldContainExactly listOf("Scheme0")
                list.templates[1].schemes.names() shouldContainExactly listOf("Scheme1", "Scheme2")
            }

            test("moves a scheme below a scheme in another template") {
                model.moveRow(4, 1, Position.BELOW)

                list.templates[0].schemes.names() shouldContainExactly listOf("Scheme0", "Scheme2", "Scheme1")
                list.templates[1].schemes.names() shouldContainExactly emptyList()
            }
        }
    }

    context("isDropInto") {
        test("returns `false` if the node can be dropped into the other") {
            model.isDropInto(null, 1, 3) shouldBe true
        }

        test("returns `true` if the node can be dropped into the other") {
            model.isDropInto(null, 0, 0) shouldBe false
        }
    }

    context("canDrop") {
        test("translates indices according to `viewIndexToModelIndex`") {
            model.canDrop(2, 3, Position.BELOW) shouldBe false

            model.viewIndexToModelIndex = { it - 1 }

            model.canDrop(2, 3, Position.BELOW) shouldBe true
        }
    }

    context("drop") {
        test("translates indices according to `viewIndexToModelIndex`") {
            val assertOriginal = { list.templates[0].schemes.names() shouldContainExactly listOf("Scheme0", "Scheme1") }
            val assertSwapped = { list.templates[0].schemes.names() shouldContainExactly listOf("Scheme1", "Scheme0") }

            assertOriginal()
            model.drop(1, 2, Position.BELOW)
            assertSwapped()
            model.drop(1, 2, Position.BELOW)
            assertOriginal()

            model.viewIndexToModelIndex = { it - 1 }

            assertOriginal()
            model.drop(2, 3, Position.BELOW)
            assertSwapped()
            model.drop(2, 3, Position.BELOW)
            assertOriginal()
        }
    }


    context("isLeaf") {
        test("throws an error if the given node is not a StateNode") {
            shouldThrow<IllegalArgumentException> { model.isLeaf("not a node") }
                .message shouldMatchBundle "template_list.error.unknown_node_type"
        }

        withData(
            mapOf(
                "is not in tree" to row({ StateNode(DummyScheme()) }, false),
                "cannot have children" to row({ model.root.children[0].children[1] }, true),
                "can have children, but has none" to row({ model.root.children[2] }, true),
                "can have children and has children" to row({ model.root.children[0] }, false),
            )
        ) { (state, expected) -> model.isLeaf(state()) shouldBe expected }
    }

    context("getChild") {
        test("throws an error if the given node is not a StateNode") {
            shouldThrow<IllegalArgumentException> { model.getChild("not a node", index = 0) }
                .message shouldMatchBundle "template_list.error.unknown_node_type"
        }

        test("throws an error if the given node is not contained in the model") {
            shouldThrow<IllegalArgumentException> { model.getChild(StateNode(DummyScheme()), 4) }
                .message shouldMatchBundle "template_list.error.node_not_in_tree"
        }

        test("throws an error if the given node cannot have children") {
            shouldThrow<IllegalStateException> { model.getChild(model.root.children[0].children[1], index = 0) }
                .message shouldMatchBundle "template_list.error.infertile_parent"
        }

        test("throws an error if there is no child at the given index") {
            shouldThrow<IndexOutOfBoundsException> { model.getChild(model.root.children[1], index = 2) }
        }

        test("returns the child at the given index") {
            model.getChild(model.root.children[1], index = 0).state shouldBe list.templates[1].schemes[0]
        }
    }

    context("getChildCount") {
        test("throws an error if the given node is not a StateNode") {
            shouldThrow<IllegalArgumentException> { model.getChildCount("not a node") }
                .message shouldMatchBundle "template_list.error.unknown_node_type"
        }

        test("throws an error if the given node is not contained in the model") {
            shouldThrow<IllegalArgumentException> { model.getChildCount(StateNode(DummyScheme())) }
                .message shouldMatchBundle "template_list.error.node_not_in_tree"
        }

        withData(
            mapOf(
                "cannot have children" to row({ model.root.children[0].children[1] }, 0),
                "can have children, but has none" to row({ model.root.children[2] }, 0),
                "can have children and has children" to row({ model.root.children[0] }, 2),
            )
        ) { (state, expected) -> model.getChildCount(state()) shouldBe expected }
    }

    context("getIndexOfChild") {
        withData(
            mapOf<String, Row3<() -> Any?, () -> Any?, Int>>(
                "parent is not a StateNode" to
                    row({ "parent" }, { model.root.children[0].children[0] }, -1),
                "child is not a StateNode" to
                    row({ model.root.children[1] }, { "child" }, -1),
                "parent is null" to
                    row({ null }, { model.root.children[1].children[0] }, -1),
                "child is null" to
                    row({ model.root.children[1] }, { null }, -1),
                "parent is not child's parent" to
                    row({ model.root.children[2] }, { model.root.children[0].children[1] }, -1),
                "parent is child's parent" to
                    row({ model.root.children[1] }, { model.root.children[1].children[0] }, 0),
                "parent matches by UUID only" to
                    row(
                        { model.root.children[0] },
                        { StateNode(list.templates[0].schemes[1].deepCopy(retainUuid = true)) },
                        1,
                    ),
            )
        ) { (parent, child, expected) -> model.getIndexOfChild(parent(), child()) shouldBe expected }
    }

    context("getParentOf") {
        test("throws an error if the node is not contained in this model") {
            shouldThrow<IllegalArgumentException> { model.getParentOf(StateNode(DummyScheme())) }
                .message shouldMatchBundle "template_list.error.node_not_in_tree"
        }

        withData(
            mapOf<String, Row2<() -> StateNode, () -> StateNode?>>(
                "parent of root" to row({ model.root }, { null }),
                "parent of template" to row({ model.root.children[0] }, { model.root }),
                "parent of scheme" to row({ model.root.children[1].children[0] }, { model.root.children[1] }),
            )
        ) { (node, expected) -> model.getParentOf(node()) shouldBe expected() }
    }

    context("getPathToRoot") {
        test("throws an error if the node is not contained in this model") {
            shouldThrow<IllegalArgumentException> { model.getPathToRoot(StateNode(DummyScheme())) }
                .message shouldMatchBundle "template_list.error.node_not_in_tree"
        }

        withData(
            mapOf(
                "path to root" to
                    row({ model.root }, { arrayOf(model.root) }),
                "path to template" to
                    row({ model.root.children[0] }, { arrayOf(model.root, model.root.children[0]) }),
                "path to scheme" to
                    row(
                        { model.root.children[1].children[0] },
                        { arrayOf(model.root, model.root.children[1], model.root.children[1].children[0]) },
                    ),
            )
        ) { (node, expected) -> model.getPathToRoot(node()).path shouldBe expected() }
    }


    context("fire methods") {
        var nodesChangedInvoked = 0
        var nodesInsertedInvoked = 0
        var nodesRemovedInvoked = 0
        var structureChangedInvoked = 0
        var totalInvoked = 0
        var lastEvent: TreeModelEvent? = null


        beforeNonContainer {
            nodesChangedInvoked = 0
            nodesInsertedInvoked = 0
            nodesRemovedInvoked = 0
            structureChangedInvoked = 0
            totalInvoked = 0
            lastEvent = null

            model.addTreeModelListener(object : TreeModelListener {
                override fun treeNodesChanged(event: TreeModelEvent) {
                    nodesChangedInvoked++
                    totalInvoked++
                    lastEvent = event
                }

                override fun treeNodesInserted(event: TreeModelEvent) {
                    nodesInsertedInvoked++
                    totalInvoked++
                    lastEvent = event
                }

                override fun treeNodesRemoved(event: TreeModelEvent) {
                    nodesRemovedInvoked++
                    totalInvoked++
                    lastEvent = event
                }

                override fun treeStructureChanged(event: TreeModelEvent) {
                    structureChangedInvoked++
                    totalInvoked++
                    lastEvent = event
                }
            })
        }


        context("fireNodeChanged") {
            test("does nothing if the given node is null") {
                model.fireNodeChanged(null)

                nodesChangedInvoked shouldBe 0
                totalInvoked - nodesChangedInvoked shouldBe 0
            }

            test("invokes the listener with an event without children if the current root is provided") {
                model.fireNodeChanged(model.root)

                nodesChangedInvoked shouldBe 1
                totalInvoked - nodesChangedInvoked shouldBe 0
                lastEvent!!.treePath.lastPathComponent shouldBe model.root
            }

            test("indicates the parent and child index when a non-root node is changed") {
                val node = model.root.children[0]

                model.fireNodeChanged(node)

                nodesChangedInvoked shouldBe 1
                totalInvoked - nodesChangedInvoked shouldBe 0
                lastEvent!!.treePath.lastPathComponent shouldBe model.root
                lastEvent!!.childIndices shouldContainExactly arrayOf(0)
                lastEvent!!.children shouldContainExactly arrayOf(node)
            }
        }

        context("fireNodeInserted") {
            test("throws an error if a template list is given") {
                val node = StateNode(DummyScheme())

                shouldThrow<IllegalArgumentException> { model.fireNodeInserted(model.root, node, 688) }
                    .message shouldMatchBundle "template_list.error.cannot_insert_root"
            }

            test("does nothing if the given node is null") {
                model.fireNodeInserted(null, StateNode(DummyScheme()), 227)

                totalInvoked shouldBe 0
            }

            test("fires an event with the given parent, node, and index") {
                val parent = model.root.children[0]
                val newNode = StateNode(DummyScheme())

                model.fireNodeInserted(newNode, parent, 1)

                nodesInsertedInvoked shouldBe 1
                totalInvoked - nodesInsertedInvoked shouldBe 0
                lastEvent!!.treePath.lastPathComponent shouldBe parent
                lastEvent!!.childIndices shouldContainExactly arrayOf(1)
                lastEvent!!.children shouldContainExactly arrayOf(newNode)
            }
        }

        context("fireNodeRemoved") {
            test("does nothing if the given node is null") {
                model.fireNodeRemoved(null, StateNode(DummyScheme()), 247)

                totalInvoked shouldBe 0
            }

            test("throws an error if a template list is given") {
                val node = StateNode(DummyScheme())

                shouldThrow<IllegalArgumentException> { model.fireNodeRemoved(model.root, node, 888) }
                    .message shouldMatchBundle "template_list.error.cannot_remove_root"
            }

            test("fires an event with the given parent, node, and index") {
                val parent = model.root.children[0]
                val oldNode = parent.children[0]

                model.fireNodeRemoved(oldNode, parent, 0)

                nodesRemovedInvoked shouldBe 1
                totalInvoked - nodesRemovedInvoked shouldBe 0
                lastEvent!!.treePath.lastPathComponent shouldBe parent
                lastEvent!!.childIndices shouldContainExactly arrayOf(0)
                lastEvent!!.children shouldContainExactly arrayOf(oldNode)
            }
        }

        context("fireNodeStructureChanged") {
            test("fires an event with the given node") {
                val node = model.root.children[0]

                model.fireNodeStructureChanged(node)

                structureChangedInvoked shouldBe 1
                totalInvoked - structureChangedInvoked shouldBe 0
                lastEvent!!.treePath.lastPathComponent shouldBe node
                lastEvent!!.childIndices should beEmptyIntArray()
                lastEvent!!.children shouldBe null
            }
        }
    }


    context("valueForPathChanged") {
        test("throws an error") {
            shouldThrow<UnsupportedOperationException> {
                model.valueForPathChanged(model.getPathToRoot(model.root), "new-value")
            }
        }
    }

    context("addTreeModelListener") {
        test("invokes the added listener when an event occurs") {
            var invoked = 0
            val listener = SimpleTreeModelListener { invoked++ }

            model.addTreeModelListener(listener)
            model.fireNodeStructureChanged()

            invoked shouldBe 1
        }
    }

    context("removeTreeModelListener") {
        test("no longer invokes the removed listener when an event occurs") {
            var invoked = 0
            val listener = SimpleTreeModelListener { invoked++ }

            model.addTreeModelListener(listener)
            model.fireNodeStructureChanged()

            model.removeTreeModelListener(listener)
            model.fireNodeStructureChanged()

            invoked shouldBe 1
        }
    }
})

/**
 * Unit tests for [StateNode].
 */
object StateNodeTest : FunSpec({
    tags(Tags.PLAIN)


    context("canHaveChildren") {
        withData(
            mapOf(
                "TemplateList" to row(TemplateList(mutableListOf()), true),
                "Template" to row(Template(), true),
                "DummyScheme" to row(DummyScheme(), false),
            )
        ) { (state, expected) -> StateNode(state).canHaveChildren shouldBe expected }
    }

    context("children") {
        context("get") {
            test("returns the templates of a template list") {
                val templates = mutableListOf(Template("Template0"), Template("Template2"))

                val node = StateNode(TemplateList(templates))

                node.children.map { it.state } shouldContainExactly templates
            }

            test("returns the schemes of a template") {
                val schemes = mutableListOf<Scheme>(DummyScheme(), DummyScheme())

                val node = StateNode(Template(schemes = schemes))

                node.children.map { it.state } shouldContainExactly schemes
            }

            test("throws an error for a non-template scheme") {
                shouldThrow<IllegalStateException> { StateNode(DummyScheme()).children }
                    .message shouldMatchBundle "template_list.error.infertile_parent"
            }
        }

        context("set") {
            test("modifies the templates of a template list") {
                val list = TemplateList(mutableListOf(Template("Template0"), Template("Template1")))

                val newTemplate = Template("NewTemplate")
                StateNode(list).children = listOf(StateNode(newTemplate))

                list.templates shouldContainExactly listOf(newTemplate)
            }

            test("modifies the schemes of a template") {
                val template = Template(schemes = mutableListOf(DummyScheme(), DummyScheme()))

                val newScheme = DummyScheme()
                StateNode(template).children = listOf(StateNode(newScheme))

                template.schemes shouldContainExactly listOf(newScheme)
            }

            test("throws an error for a non-template scheme") {
                shouldThrow<IllegalStateException> { run { StateNode(DummyScheme()).children = emptyList() } }
                    .message shouldMatchBundle "template_list.error.infertile_parent"
            }
        }
    }

    context("descendants") {
        test("returns the templates and schemes of a template list in depth-first order") {
            val templates = mutableListOf(
                Template("Template0", mutableListOf(DummyScheme("Scheme0"), DummyScheme("Scheme1"))),
                Template("Template1", mutableListOf(DummyScheme("Scheme1"))),
            )

            val children = StateNode(TemplateList(templates)).descendants()

            children.map { (it.state as Scheme).name } shouldContainExactly
                listOf("Template0", "Scheme0", "Scheme1", "Template1", "Scheme1")
        }

        test("returns the schemes of a template") {
            val schemes = mutableListOf<Scheme>(DummyScheme("Scheme0"), DummyScheme("Scheme1"))

            val children = StateNode(Template(schemes = schemes)).descendants()

            children.map { (it.state as Scheme).name } shouldContainExactly schemes.map { it.name }
        }

        test("returns an empty list if the node cannot have children") {
            StateNode(DummyScheme()).descendants() should beEmpty()
        }
    }


    context("canHaveChild") {
        withData(
            mapOf(
                "TemplateList/TemplateList" to row(TemplateList(mutableListOf()), TemplateList(mutableListOf()), false),
                "TemplateList/Template" to row(TemplateList(mutableListOf()), Template(), true),
                "TemplateList/Scheme" to row(TemplateList(mutableListOf()), DummyScheme(), false),
                "Template/TemplateList" to row(Template(), TemplateList(mutableListOf()), false),
                "Template/Template" to row(Template(), Template(), false),
                "Template/Scheme" to row(Template(), DummyScheme(), true),
                "Scheme/TemplateList" to row(DummyScheme(), TemplateList(mutableListOf()), false),
                "Scheme/Template" to row(DummyScheme(), Template(), false),
                "Scheme/Scheme" to row(DummyScheme(), DummyScheme(), false),
            )
        ) { (parent, child, expected) -> StateNode(parent).canHaveChild(StateNode(child)) shouldBe expected }
    }


    context("equals") {
        test("does not equal an object of another class") {
            StateNode(DummyScheme()) shouldNotBe 299
        }

        test("equals itself") {
            val node = StateNode(DummyScheme())

            node shouldBe node
        }

        test("equals a node of a copy with the same UUID") {
            val scheme = DummyScheme()

            StateNode(scheme) shouldBe StateNode(scheme.deepCopy(retainUuid = true))
        }

        test("does not equal a node of a copy with a different UUID") {
            val scheme = DummyScheme()

            StateNode(scheme) shouldNotBe StateNode(scheme.deepCopy(retainUuid = false))
        }

        test("does not equal a different object with a different UUID") {
            StateNode(DummyScheme()) shouldNotBe StateNode(DummyScheme())
        }
    }

    context("hashCode") {
        test("is deterministic") {
            val node = StateNode(DummyScheme())

            node.hashCode() shouldBe node.hashCode()
        }

        test("equals the hashcode of an identical object") {
            val scheme = DummyScheme()

            val node1 = StateNode(scheme)
            val node2 = StateNode(scheme)

            node1.hashCode() shouldBe node2.hashCode()
        }

        test("equals the hashcode of a deep copy with the same UUID") {
            val scheme = DummyScheme()

            val node1 = StateNode(scheme)
            val node2 = StateNode(scheme.deepCopy(retainUuid = true))

            node1.hashCode() shouldBe node2.hashCode()
        }

        test("does not equal the hashcode of a deep copy with the same UUID") {
            val scheme = DummyScheme()

            val node1 = StateNode(scheme)
            val node2 = StateNode(scheme.deepCopy(retainUuid = false))

            node1.hashCode() shouldNotBe node2.hashCode()
        }

        test("does not equal the hashcode of a different object") {
            val node1 = StateNode(DummyScheme())
            val node2 = StateNode(DummyScheme())

            node1.hashCode() shouldNotBe node2.hashCode()
        }
    }
})
