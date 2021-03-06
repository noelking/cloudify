/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.esc.driver.provisioning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.dsl.cloud.FileTransferModes;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.admin.zone.config.AnyZonesConfig;
import org.openspaces.admin.zone.config.AtLeastOneZoneConfigurer;
import org.openspaces.admin.zone.config.ZonesConfig;
import org.openspaces.core.util.StringProperties;
import org.openspaces.grid.gsm.capacity.CapacityRequirement;
import org.openspaces.grid.gsm.capacity.CapacityRequirements;
import org.openspaces.grid.gsm.capacity.CpuCapacityRequirement;
import org.openspaces.grid.gsm.capacity.DriveCapacityRequirement;
import org.openspaces.grid.gsm.capacity.MemoryCapacityRequirement;

/************
 * Cloud Provisioning configuration.
 * 
 * @author barakme
 * @since 2.0
 * 
 */
public class CloudifyMachineProvisioningConfig implements ElasticMachineProvisioningConfig {

	private static final String COLON_CHAR = ":";
	private static final String REMOTE_ADMIN_SHARE_CHAR = "$";
	private static final String BACK_SLASH = "\\";
	private static final String FORWARD_SLASH = "/";
	private static final double NUMBER_OF_CPU_CORES_PER_MACHINE_DEFAULT = 4;
	private static final String NUMBER_OF_CPU_CORES_PER_MACHINE_KEY = "number-of-cpu-cores-per-machine";

	private static final String ZONES_KEY = "zones";
	private static final String ZONES_SEPARATOR = ",";
	private static final String[] ZONES_DEFAULT = new String[] {}; // empty zone by default

	private static final boolean DEDICATED_MANAGEMENT_MACHINES_DEFAULT = false;
	private static final String DEDICATED_MANAGEMENT_MACHINES_KEY = "dedicated-management-machines";

	private static final String RESERVED_MEMORY_CAPACITY_PER_MACHINE_MEGABYTES_KEY =
			"reserved-memory-capacity-per-machine-megabytes";
	private static final long RESERVED_MEMORY_CAPACITY_PER_MACHINE_MEGABYTES_DEFAULT = 256;

	private static final String RESERVED_CPU_PER_MACHINE_KEY = "reserved-cpu-cores-per-machine";
	private static final double RESERVED_CPU_PER_MACHINE_DEFAULT = 0.0;

	private static final String RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_KEY =
			"resereved-drives-capacity-per-machine-megabytes";
	private static final Map<String, String> RESERVED_DRIVES_CAPACITY_PER_MACHINE_DEFAULT =
			new HashMap<String, String>();
	private static final String RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_KEY_VALUE_SEPERATOR = "=";
	private static final String RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_PAIR_SEPERATOR = ",";

	private static final String LOCATOR_KEY = "locator";
	private static final String SERVICE_CLOUD_CONFIGURATION_KEY = "SERVICE_CLOUD_CONFIGURATION_KEY";

	private StringProperties properties = new StringProperties(new HashMap<String, String>());

	/****************
	 * Constructor.
	 * 
	 * @param cloud .
	 * @param template .
	 * @param cloudFileContents .
	 * @param cloudTemplateName .
	 */
	public CloudifyMachineProvisioningConfig(final Cloud cloud, final CloudTemplate template,
			final String cloudTemplateName, final String managementTemplateRemoteDirectory) {

		setMinimumNumberOfCpuCoresPerMachine(template.getNumberOfCores());

		setReservedMemoryCapacityPerMachineInMB(cloud.getProvider().getReservedMemoryCapacityPerMachineInMB());
		
		String remoteDir = managementTemplateRemoteDirectory;
		logger.log(Level.FINE, "Original remote directory is: " + remoteDir);
		if (template.getFileTransfer().equals(FileTransferModes.CIFS)) {
			logger.log(Level.FINE, "Running on windows, modifying remote directory config. Original was: " + remoteDir);
			remoteDir = getWindowsRemoteDirPath(managementTemplateRemoteDirectory);
		}
		logger.log(Level.INFO, "Setting cloud configuration directory to: " + remoteDir);
		setCloudConfigurationDirectory(remoteDir);
		setCloudTemplateName(cloudTemplateName);

	}
	
	private String getWindowsRemoteDirPath(String remoteDirectory) {
		String homeDirectoryName = remoteDirectory;
		homeDirectoryName = homeDirectoryName.replace(REMOTE_ADMIN_SHARE_CHAR, "");
		if (homeDirectoryName.startsWith(FORWARD_SLASH)) {
			homeDirectoryName = homeDirectoryName.substring(1);
		}
		if (homeDirectoryName.charAt(1) == FORWARD_SLASH.charAt(0)) {
			homeDirectoryName = homeDirectoryName.substring(0, 1) + COLON_CHAR + homeDirectoryName.substring(1);
		}
		homeDirectoryName = homeDirectoryName.replace(FORWARD_SLASH, BACK_SLASH);
		return homeDirectoryName;
	}

	private static final java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(CloudifyMachineProvisioningConfig.class.getName());

	/**************
	 * .
	 * 
	 * @param properties .
	 */
	public CloudifyMachineProvisioningConfig(final Map<String, String> properties) {
		this.properties = new StringProperties(properties);
	}

	@Override
	public String getBeanClassName() {
		return ElasticMachineProvisioningCloudifyAdapter.class.getName();
	}

	@Override
	public void setProperties(final Map<String, String> properties) {
		this.properties = new StringProperties(properties);
	}

	@Override
	public Map<String, String> getProperties() {
		return this.properties.getProperties();
	}

	@Override
	public double getMinimumNumberOfCpuCoresPerMachine() {
		return properties.getDouble(
				NUMBER_OF_CPU_CORES_PER_MACHINE_KEY, NUMBER_OF_CPU_CORES_PER_MACHINE_DEFAULT);
	}

	/**********
	 * Setter.
	 * 
	 * @param minimumCpuCoresPerMachine .
	 */
	public final void setMinimumNumberOfCpuCoresPerMachine(final double minimumCpuCoresPerMachine) {
		properties.putDouble(
				NUMBER_OF_CPU_CORES_PER_MACHINE_KEY, minimumCpuCoresPerMachine);
	}

	public String getCloudTemplateName() {
		return properties.get(CloudifyConstants.ELASTIC_PROPERTIES_CLOUD_TEMPLATE_NAME);
	}

	/***************
	 * Setter.
	 * 
	 * @param cloudTemplateName .
	 */
	public final void setCloudTemplateName(final String cloudTemplateName) {
		properties.put(
				CloudifyConstants.ELASTIC_PROPERTIES_CLOUD_TEMPLATE_NAME, cloudTemplateName);
	}

	public String getCloudConfigurationDirectory() {
		return properties.get(CloudifyConstants.ELASTIC_PROPERTIES_CLOUD_CONFIGURATION_DIRECTORY);
	}

	/***************
	 * Setter.
	 * 
	 * @param cloudConfigurationDirectory .
	 */
	public final void setCloudConfigurationDirectory(final String cloudConfigurationDirectory) {
		properties.put(
				CloudifyConstants.ELASTIC_PROPERTIES_CLOUD_CONFIGURATION_DIRECTORY, cloudConfigurationDirectory);
	}

	@Override
	public CapacityRequirements getReservedCapacityPerMachine() {
		final List<CapacityRequirement> requirements = new ArrayList<CapacityRequirement>();
		requirements.add(new MemoryCapacityRequirement(getReservedMemoryCapacityPerMachineInMB()));
		requirements.add(new CpuCapacityRequirement(getReservedCpuCapacityPerMachine()));
		final Map<String, Long> reservedDriveCapacity = getReservedDriveCapacityPerMachineInMB();
		for (final Entry<String, Long> entry : reservedDriveCapacity.entrySet()) {
			final String drive = entry.getKey();
			requirements.add(new DriveCapacityRequirement(drive, entry.getValue()));
		}
		return new CapacityRequirements(requirements.toArray(new CapacityRequirement[requirements.size()]));

	}

	/**********
	 * .
	 * 
	 * @return .
	 */
	public Map<String, Long> getReservedDriveCapacityPerMachineInMB() {
		final Map<String, String> reserved =
				this.properties.getKeyValuePairs(
						RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_KEY,
						RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_PAIR_SEPERATOR,
						RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_KEY_VALUE_SEPERATOR,
						RESERVED_DRIVES_CAPACITY_PER_MACHINE_DEFAULT);

		final Map<String, Long> reservedInMB = new HashMap<String, Long>();
		for (final Map.Entry<String, String> entry : reserved.entrySet()) {
			final String drive = entry.getKey();
			reservedInMB.put(
					drive, Long.valueOf(entry.getValue()));
		}

		return reservedInMB;

	}

	/**********
	 * Setter.
	 * 
	 * @param reservedCpu .
	 */
	public void setReservedCpuCapacityPerMachineInMB(final double reservedCpu) {
		this.properties.putDouble(
				RESERVED_CPU_PER_MACHINE_KEY, reservedCpu);
	}

	public double getReservedCpuCapacityPerMachine() {
		return this.properties.getDouble(
				RESERVED_CPU_PER_MACHINE_KEY, RESERVED_CPU_PER_MACHINE_DEFAULT);
	}

	public long getReservedMemoryCapacityPerMachineInMB() {
		return this.properties.getLong(
				RESERVED_MEMORY_CAPACITY_PER_MACHINE_MEGABYTES_KEY,
				RESERVED_MEMORY_CAPACITY_PER_MACHINE_MEGABYTES_DEFAULT);
	}

	/*************
	 * Setter.
	 * 
	 * @param reservedInMB .
	 */
	public final void setReservedMemoryCapacityPerMachineInMB(final long reservedInMB) {
		this.properties.putLong(
				RESERVED_MEMORY_CAPACITY_PER_MACHINE_MEGABYTES_KEY, reservedInMB);
	}

	@Override
	public ZonesConfig getGridServiceAgentZones() {
		final String[] zones = properties.getArray(
				ZONES_KEY, ZONES_SEPARATOR, ZONES_DEFAULT);
		if (zones.length == 0) {
			// this is ok, since a pu without any zone CAN be deployed on any agent
			return new AnyZonesConfig();
		}
		return new AtLeastOneZoneConfigurer().addZones(zones).create();
	}

	/************
	 * Setter.
	 * 
	 * @param zones .
	 */
	public void setGridServiceAgentZones(final String[] zones) {
		this.properties.putArray(
				ZONES_KEY, zones, ZONES_SEPARATOR);
	}

	@Override
	public boolean isDedicatedManagementMachines() {
		return properties.getBoolean(
				DEDICATED_MANAGEMENT_MACHINES_KEY, DEDICATED_MANAGEMENT_MACHINES_DEFAULT);
	}

	/************
	 * Setter.
	 * 
	 * @param value .
	 */
	public void setDedicatedManagementMachines(final boolean value) {
		properties.putBoolean(
				DEDICATED_MANAGEMENT_MACHINES_KEY, value);
	}

	@Override
	public boolean isGridServiceAgentZoneMandatory() {
		return false;
	}

	/***********
	 * Setter.
	 * 
	 * @param locator .
	 */
	public void setLocator(final String locator) {
		properties.put(
				LOCATOR_KEY, locator);
	}

	public String getLocator() {
		return properties.get(LOCATOR_KEY);
	}

	/************
	 * Setter.
	 * 
	 * @param serviceCloudConfiguration .
	 */
	public void setServiceCloudConfiguration(final byte[] serviceCloudConfiguration) {
		final String encodedResult = jcifs.util.Base64.encode(serviceCloudConfiguration);

		properties.put(SERVICE_CLOUD_CONFIGURATION_KEY, encodedResult);
	}

	/**********
	 * Getter.
	 * 
	 * @return .
	 */
	public byte[] getServiceCloudConfiguration() {
		final String encodedFile = properties.get(SERVICE_CLOUD_CONFIGURATION_KEY);
		if (encodedFile == null) {
			return null;
		}

		final byte[] decodedFile = jcifs.util.Base64.decode(encodedFile);
		return decodedFile;

	}
}
