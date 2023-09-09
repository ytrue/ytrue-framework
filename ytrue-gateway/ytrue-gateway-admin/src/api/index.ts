import request from '../utils/request';

export const fetchData = () => {
    return request({
        url: './table.json',
        method: 'get'
    });
};

export const gatewayServerData = () => {
    return request({
        url: './gateway_server.json',
        method: 'get'
    });
};

export const gatewayServerDetailData = () => {
    return request({
        url: './gateway_server_detail.json',
        method: 'get'
    });
};

export const gatewayDistributionData = () => {
    return request({
        url: './gateway_distribution.json',
        method: 'get'
    });
};

export const applicationSystemData = () => {
    return request({
        url: './application_system.json',
        method: 'get'
    });
};

export const applicationInterfaceData = () => {
    return request({
        url: './application_interface.json',
        method: 'get'
    });
};

export const applicationInterfaceMethodData = () => {
    return request({
        url: './application_interface_method.json',
        method: 'get'
    });
};
