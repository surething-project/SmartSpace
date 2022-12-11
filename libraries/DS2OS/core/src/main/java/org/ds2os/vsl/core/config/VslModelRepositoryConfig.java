package org.ds2os.vsl.core.config;

/**
 * Configuration values belonging to the model repository.
 *
 * @author liebald
 */
public interface VslModelRepositoryConfig extends VslCharset {

    /**
     * Returns the path of the local modelFolder. The default value is "models".
     *
     * @return path to the local model folder (e.g. ds2os/models)
     */
    @ConfigDescription(description = "Path of the folder that holds the models of the "
            + "modelRepository.", id = "modelRepository.localPath", defaultValue = "models"
                    + "", restrictions = "relative or absolute path")
    String getLocalModelFolderPath();

    /**
     * Returns the URL of the context model repository. Default is empty String.
     *
     * @return URL of the context model repository ("https://cmr.ds2os.org")
     */
    @ConfigDescription(description = "URL under which the CMR can be contacted to retrieve "
            + "locally unknown models.", id = "modelRepository.cmrUrl", defaultValue = ""
                    + "", restrictions = "not implemented yet")
    String getCMRurl();

    /**
     * Flag that describes whether the local agent is a SLMR (Site Local Model Repository) or not.
     *
     * @return True if the local agent serves as SLMR, false otherwise.
     */
    @ConfigDescription(description = "Flag that describes whether the local agent is a SLMR "
            + "(Site Local Model Repository) (1) or not (0). ", id = "modelRepository.isSLMR"
                    + "", defaultValue = "0", restrictions = "0 or 1 (boolean)")
    Boolean isSLMR();
}
