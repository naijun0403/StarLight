package com.mooner.starlight.api.core

import com.mooner.starlight.plugincore.method.Method
import com.mooner.starlight.plugincore.method.MethodFunction
import com.mooner.starlight.plugincore.method.MethodType
import com.mooner.starlight.plugincore.project.Project


class Event {

    class Message {
        companion object {
            const val MESSAGE = "message"
            const val COMMAND = "command"
        }
    }

    class Notification {
        companion object {
            const val POSTED  = "notificationPosted"
            const val REMOVED = "notificationRemoved"
        }
    }

    class Project {
        companion object {
            const val START_COMPILE = "startCompile"
            const val STATE_UPDATE  = "stateUpdate"
        }
    }

    class Activity {
        companion object {
            const val CREATE   = "activityCreate"
            const val DESTROY  = "activityDestroy"
            const val PAUSE    = "activityPause"
            const val RESUME   = "activityResume"
            const val START    = "activityStart"
            const val STOP     = "activityStop"
            const val RESTART  = "activityRestart"
        }
    }
}

class EventMethod: Method<Event>() {

    override val name: String = "Event"

    override val type: MethodType = MethodType.CLASS

    override val instanceClass: Class<Event> = Event::class.java

    override val functions: List<MethodFunction> = listOf(
        function {
            name = "Message"
            returns = Event.Message::class.java
        },
        function {
            name = "Notification"
            returns = Event.Notification::class.java
        },
        function {
            name = "Project"
            returns = Event.Project::class.java
        },
        function {
            name = "Activity"
            returns = Event.Activity::class.java
        },
    )

    override fun getInstance(project: Project): Any = Event::class.java
}