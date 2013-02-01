package org.flowvisor.api.handlers.configuration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.flowvisor.api.handlers.ApiHandler;
import org.flowvisor.api.handlers.HandlerUtils;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FlowSpace;
import org.flowvisor.config.FlowvisorImpl;
import org.flowvisor.config.SliceImpl;
import org.flowvisor.config.SwitchImpl;
import org.flowvisor.exceptions.MissingRequiredField;
import org.flowvisor.flows.FlowSpaceUtil;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class GetConfig implements ApiHandler<Map<String, Object>> {

	
	
	@Override
	public JSONRPC2Response process(Map<String, Object> params) {
		JSONRPC2Response resp = null;
		HashMap<String, Object> configs = new HashMap<String, Object>();
		
		
		try {
			String sliceName = HandlerUtils.<String>fetchField(SLICENAME, params, false, null);
			String dpidStr = HandlerUtils.<String>fetchField(FlowSpace.DPID, params, false, null);
			
			configs.put(TRACK, FlowvisorImpl.getProxy().gettrack_flows());
			configs.put(STATSDESC, FlowvisorImpl.getProxy().getstats_desc_hack());
			configs.put(TOPOCTRL, FlowvisorImpl.getProxy().getTopologyServer());
			configs.put(FSCACHE, FlowvisorImpl.getProxy().getFlowStatsCache());
			addFloodPerms(dpidStr, configs);
			addFlowmodLimits(sliceName, dpidStr, configs);
			
			resp = new JSONRPC2Response(configs, 0);
		} catch (ConfigError e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), 
					cmdName() + ": Unable to fetch/set config : " + e.getMessage()), 0);
		} catch (ClassCastException e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} catch (MissingRequiredField e) {
			resp = new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), 
					cmdName() + ": " + e.getMessage()), 0);
		} 
		return resp;
		
	}


    @SuppressWarnings("unchecked")
	private void addFlowmodLimits(String sliceName, String dpidStr,
			HashMap<String, Object> configs) throws MissingRequiredField, ConfigError {
    	List<HashMap<String, Object>> list = new LinkedList<HashMap<String,Object>>();
    	HashMap<String, Object> subconfs = new HashMap<String, Object>();
		if (sliceName != null && dpidStr != null) {
			subconfs.put(SLICENAME, sliceName);
			subconfs.put(FlowSpace.DPID, dpidStr);
			subconfs.put(FMLIMIT, SwitchImpl.getProxy().getMaxFlowMods(sliceName, 
					FlowSpaceUtil.parseDPID(dpidStr)));
			list.add(subconfs);
		} else if (sliceName != null && dpidStr == null) {
			subconfs.put(SLICENAME, sliceName);
			subconfs.put(FlowSpace.DPID, "all");
			subconfs.put(FMLIMIT, SliceImpl.getProxy().getMaxFlowMods(sliceName));
			list.add((HashMap<String, Object>)subconfs.clone());
			for (String dpid : HandlerUtils.getAllDevices()) {
				subconfs.clear();
				subconfs.put(SLICENAME, sliceName);
				subconfs.put(FlowSpace.DPID, dpid);
				subconfs.put(FMLIMIT, SwitchImpl.getProxy().getMaxFlowMods(sliceName, 
						FlowSpaceUtil.parseDPID(dpid)));
				list.add((HashMap<String, Object>)subconfs.clone());
			}
		} else if (dpidStr != null && sliceName == null) {
			long dpid = FlowSpaceUtil.parseDPID(dpidStr);
			List<String> slices = SliceImpl.getProxy().getAllSliceNames();
			for (String slice : slices) {
				subconfs.clear();
				subconfs.put(SLICENAME, slice);
				subconfs.put(FlowSpace.DPID, dpidStr);
				subconfs.put(FMLIMIT, SwitchImpl.getProxy().getMaxFlowMods(slice,dpid));
				list.add((HashMap<String, Object>)subconfs.clone());
			}
		} else {
			List<String> slices = SliceImpl.getProxy().getAllSliceNames();
			for (String slice : slices) {
				subconfs.clear();
				subconfs.put(SLICENAME, slice);
				subconfs.put(FlowSpace.DPID, "all");
				subconfs.put(FMLIMIT, SliceImpl.getProxy().getMaxFlowMods(sliceName));
				list.add((HashMap<String, Object>)subconfs.clone());
			}
		}
		configs.put(MAX, list);	
	}


	private void addFloodPerms(String dpidStr,
			HashMap<String, Object> configs) throws ConfigError {
		HashMap<String, Object> subconfs = new HashMap<String, Object>();
		if (dpidStr != null) {
			subconfs.put(SLICENAME, 
					SwitchImpl.getProxy().getFloodPerm(FlowSpaceUtil.parseDPID(dpidStr)));
			subconfs.put(FlowSpace.DPID, dpidStr);
		} else {
			subconfs.put(FlowSpace.DPID, "all");
			subconfs.put(SLICENAME, FlowvisorImpl.getProxy().getFloodPerm());
		}
		configs.put(FLOOD, subconfs);
	}

	@Override
	public JSONRPC2ParamsType getType() {
		return JSONRPC2ParamsType.OBJECT;
	}


	@Override
	public String cmdName() {
		return "set-config";
	}

}
