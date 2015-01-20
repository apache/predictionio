package io.prediction.data.api

import io.prediction.data.storage.AccessKey

case class AppRequest(
                       id: Int = 0,
                       name: String = "",
                       description: String = ""
                    )

case class AppResponse(
        id: Int = 0,
        name: String = "",
        keys: Seq[AccessKey]
                        )
case class GeneralResponse(
        status: Int = 0,
        message: String = ""
                            )

