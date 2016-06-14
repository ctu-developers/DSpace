package org.dspace.rest;

import org.apache.log4j.Logger;
import org.dspace.authority.common.AuthorityPersonIterator;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.ItemIterator;
import org.dspace.content.service.ItemService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.rest.common.Item;
import org.dspace.rest.common.authority.AuthorityPerson;
import org.dspace.rest.common.authority.Authority;
import org.dspace.rest.exceptions.ContextException;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.usage.UsageEvent;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.hp.hpl.jena.vocabulary.RSS.items;

/**
 * This resource contains endpoints for authority module.
 *
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * @version 1.0
 * @since 2015-12-03
 */
// Every DSpace class used without namespace is from package
// org.dspace.rest.common.*. Otherwise namespace is defined.
@Path("/authoritypersons")
public class AuthorityResource extends Resource {

    /** Log4j for logging. */
    private static final Logger LOGGER = Logger.getLogger(AuthorityResource.class);

    /** Default value for limit. */
    private static final String LIMIT_DEFAULT = "20";
    /** Default value for offset. */
    private static final String OFFSET_DEFAULT = "0";


    /**
     * Get authority persons from database.
     *
     * @param limit  How many persons in array.
     * @param offset Offset of persons in array.
     * @param token  If you want to access to authority persons under logged user into context. In headers must be set
     *               header "rest-dspace-token" with passed token from login method.
     *
     * @return Returns array of authority persons.
     *
     * @throws WebApplicationException It can be thrown by SQLException, when was problem with reading authority persons
     *                                 from database or ContextException, when was problem with creating context of
     *                                 DSpace.
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public final AuthorityPerson[] getAuthorityPersons(
            @QueryParam("limit") @DefaultValue("100") Integer limit,
            @QueryParam("offset") @DefaultValue("0") Integer offset,
            @Context HttpHeaders headers) throws WebApplicationException {

        LOGGER.info("Reading all authority persons.");

        org.dspace.core.Context context = null;
        AuthorityPerson[] authorityPersons = null;

        if (limit == null || limit <= 0) {
            LOGGER.warn("Limit was badly set, using default value(" + LIMIT_DEFAULT + ").");
            limit = Integer.valueOf(LIMIT_DEFAULT);
        }
        if (offset == null || offset < 0) {
            LOGGER.warn("Offset was badly set, using default value(" + OFFSET_DEFAULT + ").");
            offset = Integer.valueOf(OFFSET_DEFAULT);
        }

        try {
            context = createContext(getUser(headers));

            boolean anonymous = false;
            try {
                checkAuthorization(context);
            } catch (WebApplicationException e) {
                context = createContext(getUser(headers));
                anonymous = true;
            }

            final AuthorityPersonIterator dspaceAuthorityPersons = org.dspace.authority.common.AuthorityPerson.findAll(context);

            final List<AuthorityPerson> persons = new ArrayList<>();
            org.dspace.authority.common.AuthorityPerson dspaceAuthorityPerson;
            for (int i = 0; i < (limit + offset) && dspaceAuthorityPersons.hasNext(); i++) {
                dspaceAuthorityPerson = dspaceAuthorityPersons.next();
                if (i >= offset) {
                    persons.add(new AuthorityPerson(dspaceAuthorityPerson, anonymous));
                }
            }

            authorityPersons = persons.toArray(new AuthorityPerson[persons.size()]);
            context.complete();

        } catch (SQLException e) {
            processException("Something went wrong while reading authority persons from database. Message: " + e, context);
        } catch (ContextException e) {
            processException("Something went wrong while reading authority person, ContextException. Message: " + e.getMessage(), null);
        } catch (AuthorizeException e) {
            context.abort();
            LOGGER.error("Something went wrong while reading authority person, AuthorizeException. Message: " + e.getMessage(), null);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } finally {
            processFinally(context);
        }

        LOGGER.debug("Authority persons were successfully read.");
        return authorityPersons;
    }

    /**
     * Get authority person by uid.
     *
     * @param uid   Uid of authority person, which looking for.
     * @param token If you want to access to authority person under logged user into context. In headers must be set
     *              header "rest-dspace-token" with passed token from login method.
     *
     * @return Returns authority person if is found, otherwise returns NOT FOUND.
     *
     * @throws WebApplicationException It can be thrown by SQLException, when was problem with reading authority person
     *                                 from database or ContextException, when was problem with creating context of
     *                                 DSpace.
     */
    @GET
    @Path("/{uid}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public final AuthorityPerson getAuthorityPerson(
            @PathParam("uid") final String uid,
            @Context HttpHeaders headers) throws WebApplicationException {

        LOGGER.info("Reading authority person with key=" + uid + ".");

        org.dspace.core.Context context = null;
        AuthorityPerson authorityPerson = null;

        try {
            context = createContext(getUser(headers));

            boolean anonymous = false;
            try {
                checkAuthorization(context);
            } catch (WebApplicationException e) {
                context = createContext(getUser(headers));
                anonymous = true;
            }

            final org.dspace.authority.common.AuthorityPerson dspaceAuthorityPerson = searchForAuthorityPersonByUid(context, uid);
            authorityPerson = new AuthorityPerson(dspaceAuthorityPerson, anonymous);
            context.complete();

        } catch (SQLException e) {
            processException("Something went wrong while reading authority person from database. Message: " + e, context);
        } catch (ContextException e) {
            processException("Something went wrong while reading authority person, ContextException. Message: " + e.getMessage(), null);
        } catch (AuthorizeException e) {
            context.abort();
            LOGGER.error("Something went wrong while reading authority person, AuthorizeException. Message: " + e.getMessage(), null);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } finally {
            processFinally(context);
        }

        LOGGER.debug("Authority person were successfully read.");
        return authorityPerson;
    }

    /**
     * Get authority person authorities.
     *
     * @param uid    Uid of authority person.
     * @param limit  How many authorities in array.
     * @param offset Offset of authorities in array.
     * @param token  If you want to access to authority persons under logged user into context. In headers must be set
     *               header "rest-dspace-token" with passed token from login method.
     *
     * @return Returns array of authority person authorities.
     *
     * @throws WebApplicationException It can be thrown by SQLException, when was problem with reading authority persons
     *                                 from database or ContextException, when was problem with creating context of
     *                                 DSpace.
     */
    @GET
    @Path("/{uid}/authorities")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public final Authority[] getAuthorityPersonAuthorities(
            @PathParam("uid") final String uid,
            @QueryParam("limit") @DefaultValue(LIMIT_DEFAULT) Integer limit,
            @QueryParam("offset") @DefaultValue(OFFSET_DEFAULT) Integer offset,
            @Context HttpHeaders headers) throws WebApplicationException {

        LOGGER.info("Reading all authorities for authority person(uid=" + uid + ").");

        org.dspace.core.Context context = null;
        Authority[] authorities = null;

        if (limit == null || limit <= 0) {
            LOGGER.warn("Limit was badly set, using default value(" + LIMIT_DEFAULT + ").");
            limit = Integer.valueOf(LIMIT_DEFAULT);
        }
        if (offset == null || offset < 0) {
            LOGGER.warn("Offset was badly set, using default value(" + OFFSET_DEFAULT + ").");
            offset = Integer.valueOf(OFFSET_DEFAULT);
        }

        try {
            context = createContext(getUser(headers));

            boolean anonymous = false;
            try {
                checkAuthorization(context);
            } catch (WebApplicationException e) {
                context = createContext(getUser(headers));
                anonymous = true;
            }

            final org.dspace.authority.common.AuthorityPerson dspaceAuthorityPerson = searchForAuthorityPersonByUid(context, uid);

            final List<org.dspace.authority.common.Authority> dspaceAuthorities = dspaceAuthorityPerson.getAuthorities();
            final List<Authority> authoritiesList = new ArrayList<>();
            for (int i = 0; i < (limit + offset) && i < dspaceAuthorities.size(); i++) {
                if (i >= offset) {
                    if (!anonymous || !AuthorityPerson.FORBIDDEN_AUTHORITIES.contains(dspaceAuthorities.get(i).getName())) {
                        authoritiesList.add(new Authority(dspaceAuthorities.get(i)));
                    }
                }
            }
            authorities = authoritiesList.toArray(new Authority[authoritiesList.size()]);

            context.complete();
        } catch (SQLException e) {
            processException("Something went wrong while reading authority persons from database. Message: " + e, context);
        } catch (ContextException e) {
            processException("Something went wrong while reading authority person, ContextException. Message: " + e.getMessage(), null);
        } catch (AuthorizeException e) {
            context.abort();
            LOGGER.error("Something went wrong while reading authority person, AuthorizeException. Message: " + e.getMessage(), null);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } finally {
            processFinally(context);
        }

        LOGGER.debug("Authority person authorities were successfully read.");
        return authorities;
    }

    /**
     * Get items for authority person.
     *
     * @param uid    Uid of authority person.
     * @param limit  How many authorities in array.
     * @param offset Offset of authorities in array.
     * @param token  If you want to access to authority persons under logged user into context. In headers must be set
     *               header "rest-dspace-token" with passed token from login method.
     *
     * @return Returns array of authority person authorities.
     *
     * @throws WebApplicationException It can be thrown by SQLException, when was problem with reading authority persons
     *                                 from database or ContextException, when was problem with creating context of
     *                                 DSpace.
     */
    @GET
    @Path("/{uid}/items")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public final Item[] getAuthorityPersonItems(
            @PathParam("uid") final String uid,
            @QueryParam("expand") String expand,
            @QueryParam("limit") @DefaultValue(LIMIT_DEFAULT) Integer limit,
            @QueryParam("offset") @DefaultValue(OFFSET_DEFAULT) Integer offset,
            @Context HttpHeaders headers) throws WebApplicationException {

        LOGGER.info("Reading all items for authority person(uid=" + uid + ").");

        org.dspace.core.Context context = null;
        final List<Item> itemList = new ArrayList<>();

        if (limit == null || limit <= 0) {
            LOGGER.warn("Limit was badly set, using default value(" + LIMIT_DEFAULT + ").");
            limit = Integer.valueOf(LIMIT_DEFAULT);
        }
        if (offset == null || offset < 0) {
            LOGGER.warn("Offset was badly set, using default value(" + OFFSET_DEFAULT + ").");
            offset = Integer.valueOf(OFFSET_DEFAULT);
        }

        try {
            context = createContext(getUser(headers));
            int count = 0;

            String sql_metadata_items =
                    "SELECT RESOURCE_ID \n"
                            + "FROM METADATAVALUE\n"
                            + " WHERE RESOURCE_TYPE_ID = 2\n"
                            + " AND AUTHORITY = '" + uid + "'";

            final TableRowIterator iterator = org.dspace.storage.rdbms.DatabaseManager.queryTable(context, "METADATAVALUE", sql_metadata_items);
            while (iterator.hasNext()) {
                TableRow row = iterator.next();
                if (count >= offset && (count - offset) < limit) {
                    org.dspace.content.Item dspaceItem = org.dspace.content.Item.find(context, row.getIntColumn("RESOURCE_ID"));
                    if (ItemService.isItemListedForUser(context, dspaceItem)) {
                        Item item = new Item(dspaceItem, expand, context);
                        itemList.add(item);
                    }
                }
                count++;
            }

            context.complete();
        } catch (SQLException e) {
            processException("Something went wrong while reading items for authority persons from database. Message: " + e, context);
        } catch (ContextException e) {
            processException("Something went wrong while reading items authority person, ContextException. Message: " + e.getMessage(), null);
        } finally {
            processFinally(context);
        }

        LOGGER.debug("Authority person authorities were successfully read.");
        return itemList.toArray(new Item[itemList.size()]);
    }

    /**
     * Get authority key for authority person.
     *
     * @param uid             Uid of authority person, which looking for.
     * @param nameOfAuthority Name of authority.
     * @param token           If you want to access to authority person under logged user into context. In headers must
     *                        be set header "rest-dspace-token" with passed token from login method.
     *
     * @return Returns key of authority for authority person with uid.
     *
     * @throws WebApplicationException It can be thrown by SQLException, when was problem with reading authority person
     *                                 from database or ContextException, when was problem with creating context of
     *                                 DSpace.
     */
    @GET
    @Path("/{uid}/authorities/{nameOfAuthority}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public final String getAuthorityPersonKey(
            @PathParam("uid") final String uid, @PathParam("nameOfAuthority") final String nameOfAuthority,
            @Context HttpHeaders headers) throws WebApplicationException {

        LOGGER.info("Reading authority in authority person with key=" + uid + ".");

        org.dspace.core.Context context = null;
        String key = "";

        try {
            context = createContext(getUser(headers));

            try {
                checkAuthorization(context);
            } catch (WebApplicationException e) {
                if (AuthorityPerson.FORBIDDEN_AUTHORITIES.contains(nameOfAuthority)) {
                    throw e;
                }
                context = createContext(getUser(headers));
            }

            final org.dspace.authority.common.AuthorityPerson dspaceAuthorityPerson = searchForAuthorityPersonByUid(context, uid);
            key = getAuthorityFromPerson(context, dspaceAuthorityPerson, nameOfAuthority).getKey();
            context.complete();

        } catch (SQLException e) {
            processException("Something went wrong while reading authority in authority person from database. Message: " + e, context);
        } catch (ContextException e) {
            processException("Something went wrong while reading authority in authority person, ContextException. Message: " + e.getMessage(), null);
        } catch (AuthorizeException e) {
            context.abort();
            LOGGER.error("Something went wrong while reading authority in authority person, AuthorizeException. Message: " + e.getMessage(), null);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } finally {
            processFinally(context);
        }

        LOGGER.debug("Authority person key were successfully read.");
        return key;
    }


    /**
     * Create authority person in database.
     *
     * @param authorityPerson Authority person which will be created. If uid is empty it will be generated.
     * @param token           If you want to create authority person you must be logged into context. In headers must be
     *                        set header "rest-dspace-token" with passed token from login method.
     *
     * @return Returns authority person which was created.
     *
     * @throws WebApplicationException It can be thrown by SQLException, when was problem with creating authority person
     *                                 in database or ContextException, when was problem with creating context of
     *                                 DSpace.
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public final AuthorityPerson createAuthorityPerson(
            final AuthorityPerson authorityPerson,
            @Context HttpHeaders headers) throws WebApplicationException {

        LOGGER.info("Creating authority person in database.");

        org.dspace.core.Context context = null;
        AuthorityPerson retAuthorityPerson = null;

        try {
            context = createContext(getUser(headers));

            final org.dspace.authority.common.AuthorityPerson dspaceAuthorityPerson =
                    org.dspace.authority.common.AuthorityPerson.create(context);

            if (authorityPerson.getUid() == null) {
                dspaceAuthorityPerson.generateUID();
            } else {
                dspaceAuthorityPerson.setUid(authorityPerson.getUid());
            }

            if (stringsNotEmpty(authorityPerson.getFirstName(), authorityPerson.getLastName())) {
                dspaceAuthorityPerson.setFirstName(authorityPerson.getFirstName());
                dspaceAuthorityPerson.setLastName(authorityPerson.getLastName());
                dspaceAuthorityPerson.update();
            } else {
                context.abort();
                LOGGER.error("Authority person could not be without name!");
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }

            retAuthorityPerson = new AuthorityPerson(dspaceAuthorityPerson, false);

            context.complete();
        } catch (SQLException e) {
            processException("Something went wrong while creating authority person in database. Message: " + e, context);
        } catch (ContextException e) {
            processException("Something went wrong while creating authority person, ContextException. Message: " + e.getMessage(), null);
        } catch (AuthorizeException e) {
            context.abort();
            LOGGER.error("Something went wrong while creating authority person, AuthorizeException. Message: " + e.getMessage(), null);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } finally {
            processFinally(context);
        }

        LOGGER.debug("Creation of authority person was succesfull.");
        return retAuthorityPerson;
    }

    /**
     * Create authority in authority person.
     *
     * @param uid       Uid of authority person in which will be created authority.
     * @param authority Authority which will be created.
     * @param token     If you want to create authority you must be logged into context. In headers must be set header
     *                  "rest-dspace-token" with passed token from login method.
     *
     * @return Returns status OK if all was ok. Otherwise another code.
     *
     * @throws WebApplicationException It can be thrown by SQLException, when was problem with creating authority in
     *                                 database or ContextException, when was problem with creating context of DSpace.
     */
    @POST
    @Path("/{uid}/authorities")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public final Response createAuthorityInAuthorityPerson(
            @PathParam("uid") final String uid, final Authority authority,
            @Context HttpHeaders headers) throws WebApplicationException {
        LOGGER.info("Creating authority in authority person.");

        org.dspace.core.Context context = null;

        try {
            context = createContext(getUser(headers));

            final org.dspace.authority.common.AuthorityPerson dspaceAuthorityPerson =
                    searchForAuthorityPersonByUid(context, uid);
            final org.dspace.authority.common.Authority dspaceAuthority = org.dspace.authority.common.Authority.create(context);

            if (stringsNotEmpty(authority.getName(), authority.getKey())) {
                dspaceAuthority.setName(authority.getName());
                dspaceAuthority.setKey(authority.getKey());
                dspaceAuthority.update();
                dspaceAuthorityPerson.addAuthority(dspaceAuthority);
            } else {
                context.abort();
                LOGGER.error("Authority could not be without name and key!");
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
            context.complete();
        } catch (SQLException e) {
            processException("Something went wrong while creating authority in authority person in database. Message: " + e, context);
        } catch (ContextException e) {
            processException("Something went wrong while creating authority in authority person, ContextException. Message: " + e.getMessage(), null);
        } catch (AuthorizeException e) {
            context.abort();
            LOGGER.error("Something went wrong while creating authority in authority person, AuthorizeException. Message: " + e.getMessage(), null);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } finally {
            processFinally(context);
        }

        LOGGER.debug("Creation of authority in authority person was successful.");
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Update authority person in database.
     *
     * @param uid             Uid of authority person.
     * @param authorityPerson Authority person which will be update.
     * @param token           If you want to update authority person you must be logged into context. In headers must be
     *                        set header "rest-dspace-token" with passed token from login method.
     *
     * @return Returns status OK if all was ok. Otherwise another code.
     *
     * @throws WebApplicationException It can be thrown by SQLException, when was problem with upadting authority person
     *                                 in database or ContextException, when was problem with creating context of
     *                                 DSpace.
     */
    @PUT
    @Path("/{uid}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public final Response updateAuthorityPerson(
            @PathParam("uid") final String uid, final AuthorityPerson authorityPerson,
            @Context HttpHeaders headers) throws WebApplicationException {

        LOGGER.info("Updating authority person in database.");

        org.dspace.core.Context context = null;

        try {
            context = createContext(getUser(headers));

            final org.dspace.authority.common.AuthorityPerson dspaceAuthorityPerson = searchForAuthorityPersonByUid(context, uid);
            dspaceAuthorityPerson.setUid(authorityPerson.getUid());
            dspaceAuthorityPerson.setFirstName(authorityPerson.getFirstName());
            dspaceAuthorityPerson.setLastName(authorityPerson.getLastName());
            dspaceAuthorityPerson.update();

            context.complete();
        } catch (SQLException e) {
            processException("Something went wrong while updating authority person in database. Message: " + e, context);
        } catch (ContextException e) {
            processException("Something went wrong while updating authority person, ContextException. Message: " + e.getMessage(), null);
        } catch (AuthorizeException e) {
            context.abort();
            LOGGER.error("Something went wrong while updating authority person, AuthorizeException. Message: " + e.getMessage(), null);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } finally {
            processFinally(context);
        }

        LOGGER.debug("Updating authority person was successful.");
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Update authority in authority person.
     *
     * @param uid             Uid for authority person in which will be authority updated.
     * @param nameOfAuthority Name of authority which will be updated.
     * @param authority       Authority which will be saved.
     * @param token           If you want to update authority in authority person you must be logged into context. In
     *                        headers must be set header "rest-dspace-token" with passed token from login method.
     *
     * @return Returns status OK if all was ok. Otherwise another code.
     *
     * @throws WebApplicationException It can be thrown by SQLException, when was problem with upadting authority person
     *                                 in database or ContextException, when was problem with creating context of
     *                                 DSpace.
     */
    @PUT
    @Path("/{uid}/authorities/{nameOfAuthority}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public final Response updateAuthorityInAuthorityPerson(
            @PathParam("uid") final String uid, @PathParam("nameOfAuthority") final String nameOfAuthority,
            final Authority authority,
            @Context HttpHeaders headers) throws WebApplicationException {

        LOGGER.info("Updating authority(" + nameOfAuthority + " in authority person(" + uid + ".");
        org.dspace.core.Context context = null;

        try {
            context = createContext(getUser(headers));

            final org.dspace.authority.common.AuthorityPerson dspaceAuthorityPerson = searchForAuthorityPersonByUid(context, uid);
            final org.dspace.authority.common.Authority dspaceAuthority = getAuthorityFromPerson(context, dspaceAuthorityPerson, nameOfAuthority);
            dspaceAuthority.setName(authority.getName());
            dspaceAuthority.setKey(authority.getKey());
            dspaceAuthority.update();

            context.complete();
        } catch (SQLException e) {
            processException("Something went wrong while updating authority in authority person in database. Message: " + e, context);
        } catch (ContextException e) {
            processException("Something went wrong while updating authority in authority person, ContextException. Message: " + e.getMessage(), null);
        } catch (AuthorizeException e) {
            context.abort();
            LOGGER.error("Something went wrong while updating authority in authority person, AuthorizeException. Message: " + e.getMessage(), null);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } finally {
            processFinally(context);
        }

        LOGGER.debug("Updating of authority in authority person was successful.");
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Delete authority person.
     *
     * @param uid   Uid authority person which will be deleted.
     * @param token If you want to delete authority person you must be logged into context. In headers must be set
     *              header "rest-dspace-token" with passed token from login method.
     *
     * @return Returns status OK if all was ok. Otherwise another code.
     *
     * @throws WebApplicationException It can be thrown by SQLException, when was problem with deleting authority person
     *                                 in database or ContextException, when was problem with creating context of
     *                                 DSpace.
     */
    @DELETE
    @Path("/{uid}")
    public final Response deleteAuthorityPerson(
            @PathParam("uid") final String uid,
            @Context HttpHeaders headers) throws WebApplicationException {

        LOGGER.info("Deleting authority person from database.");

        org.dspace.core.Context context = null;

        try {
            context = createContext(getUser(headers));

            final org.dspace.authority.common.AuthorityPerson dspaceAuthorityPerson = searchForAuthorityPersonByUid(context, uid);
            dspaceAuthorityPerson.delete();

            context.complete();
        } catch (SQLException e) {
            processException("Something went wrong while deleting authority person from database. Message: " + e, context);
        } catch (ContextException e) {
            processException("Something went wrong while deleting authority person, ContextException. Message: " + e.getMessage(), null);
        } catch (AuthorizeException e) {
            context.abort();
            LOGGER.error("Something went wrong while deleting authority person, AuthorizeException. Message: " + e.getMessage(), null);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } finally {
            processFinally(context);
        }

        LOGGER.debug("Deleting authority person was successful.");
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Delete authority in authority person.
     *
     * @param uid             Uid authority person in which will be authority deleted.
     * @param nameOfAuthority Name of authority which will be deleted in person.
     * @param token           If you want to delete authority in authority person you must be logged into context. In
     *                        headers must be set header "rest-dspace-token" with passed token from login method.
     *
     * @return Returns status OK if all was ok. Otherwise another code.
     *
     * @throws WebApplicationException It can be thrown by SQLException, when was problem with deleting authority in
     *                                 authority person in database or ContextException, when was problem with creating
     *                                 context of DSpace.
     */
    @DELETE
    @Path("/{uid}/authorities/{nameOfAuthority}")
    public final Response deleteAuthorityInAuthorityPerson(
            @PathParam("uid") final String uid, @PathParam("nameOfAuthority") final String nameOfAuthority,
            @Context HttpHeaders headers) throws WebApplicationException {

        LOGGER.info("Deleting authority(name=" + nameOfAuthority + ") from authority person{uid=" + uid + ").");

        org.dspace.core.Context context = null;

        try {
            context = createContext(getUser(headers));

            final org.dspace.authority.common.AuthorityPerson dspaceAuthorityPerson = searchForAuthorityPersonByUid(context, uid);
            final org.dspace.authority.common.Authority dspaceAuthority = getAuthorityFromPerson(context, dspaceAuthorityPerson, nameOfAuthority);
            dspaceAuthority.delete();

            context.complete();
        } catch (SQLException e) {
            processException("Something went wrong while deleting authority in authority person(uid=" + uid + "). Message: " + e, context);
        } catch (ContextException e) {
            processException("Something went wrong while deleting authority in authority person(uid=" + uid + "), ContextException. Message: " + e.getMessage(), null);
        } catch (AuthorizeException e) {
            context.abort();
            LOGGER.error("Something went wrong while deleting authority in authority person(uid=" + uid + "), AuthorizeException. Message: " + e.getMessage(), null);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } finally {
            processFinally(context);
        }

        LOGGER.debug("Deleting authority from authority person was successful.");
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Search for authority person by authority.
     *
     * @param authority Authority after by will be authority person searched for.
     * @param token     If you want to access to authority persons under logged user into context. In headers must be
     *                  set header "rest-dspace-token" with passed token from login method.
     *
     * @return Returns array of authority persons.
     *
     * @throws WebApplicationException It can be thrown by SQLException, when was problem with reading authority persons
     *                                 from database or ContextException, when was problem with creating context of
     *                                 DSpace.
     */
    @POST
    @Path("/search-by-authority")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public final AuthorityPerson searchForAuthorityPersonByKey(
            final Authority authority,
            @Context HttpHeaders headers) throws WebApplicationException {

        LOGGER.info("Searching for authority person with authorityName=" + authority.getName() + " and key=" + authority.getKey() + ".");

        org.dspace.core.Context context = null;
        AuthorityPerson authorityPerson = null;

        try {
            context = createContext(getUser(headers));

            boolean anonymous = false;
            try {
                checkAuthorization(context);
            } catch (WebApplicationException e) {
                anonymous = true;
                context = createContext(getUser(headers));
            }

            final org.dspace.authority.common.AuthorityPerson dspaceAuthorityPerson =
                    org.dspace.authority.common.AuthorityPerson.findByKey(context, authority.getName(), authority.getKey());

            if (dspaceAuthorityPerson != null) {
                authorityPerson = new AuthorityPerson(dspaceAuthorityPerson, anonymous);
            }

            context.complete();
        } catch (SQLException e) {
            processException("Something went wrong while searching for authority person from database. Message: " + e, context);
        } catch (ContextException e) {
            processException("Something went wrong while searching for authority person, ContextException. Message: " + e.getMessage(), null);
        } catch (AuthorizeException e) {
            context.abort();
            LOGGER.error("Something went wrong while searching for authority person, AuthorizeException. Message: " + e.getMessage(), null);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } finally {
            processFinally(context);
        }

        LOGGER.debug("Authority person were successfully searched.");
        if (authorityPerson == null) {
            LOGGER.debug("Authority with with authorityName=" + authority.getName() + " and key=" + authority.getKey() + " not found.");
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } else {
            LOGGER.debug("Authority with with authorityName=" + authority.getName() + " and key=" + authority.getKey() + " found.");
        }

        return authorityPerson;
    }

    /**
     * Search for authority person by name.
     *
     * @param name   Name of authority person in this format "LastName, FirstName".
     * @param limit  How many persons in array.
     * @param offset Offset of persons in array.
     * @param token  If you want to access to authority persons under logged user into context. In headers must be set
     *               header "rest-dspace-token" with passed token from login method.
     *
     * @return Returns array of authority persons.
     *
     * @throws WebApplicationException It can be thrown by SQLException, when was problem with reading authority persons
     *                                 from database or ContextException, when was problem with creating context of
     *                                 DSpace.
     */
    @POST
    @Path("/search-by-name")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public final AuthorityPerson[] searchAuthorityPersonByName(
            final String name,
            @QueryParam("limit") @DefaultValue(LIMIT_DEFAULT) Integer limit,
            @QueryParam("offset") @DefaultValue(OFFSET_DEFAULT) Integer offset,
            @Context HttpHeaders headers) throws WebApplicationException {

        LOGGER.info("Searching for authority person with name=" + name + ".");

        org.dspace.core.Context context = null;
        AuthorityPerson[] authorityPersons = null;

        if (limit == null || limit <= 0) {
            LOGGER.warn("Limit was badly set, using default value(" + LIMIT_DEFAULT + ").");
            limit = Integer.valueOf(LIMIT_DEFAULT);
        }
        if (offset == null || offset < 0) {
            LOGGER.warn("Offset was badly set, using default value(" + OFFSET_DEFAULT + ").");
            offset = Integer.valueOf(OFFSET_DEFAULT);
        }

        try {
            context = createContext(getUser(headers));

            boolean anonymous = false;
            try {
                checkAuthorization(context);
            } catch (WebApplicationException e) {
                anonymous = true;
                context = createContext(getUser(headers));
            }

            final AuthorityPersonIterator dspaceAuthorityPersons =
                    org.dspace.authority.common.AuthorityPerson.findByName(context,
                            name.substring(name.lastIndexOf(',') + 1),
                            name.substring(0, name.indexOf(',')));
            final List<AuthorityPerson> authorityPersonList = new ArrayList<>();

            org.dspace.authority.common.AuthorityPerson dspaceAuthorityPerson;
            for (int i = 0; i < (limit + offset) && dspaceAuthorityPersons.hasNext(); i++) {
                dspaceAuthorityPerson = dspaceAuthorityPersons.next();
                if (i >= offset) {
                    authorityPersonList.add(new AuthorityPerson(dspaceAuthorityPerson, anonymous));
                }
            }

            authorityPersons = authorityPersonList.toArray(new AuthorityPerson[authorityPersonList.size()]);

            context.complete();
        } catch (SQLException e) {
            processException("Something went wrong while searching for authority person from database. Message: " + e, context);
        } catch (ContextException e) {
            processException("Something went wrong while searching for authority person, ContextException. Message: " + e.getMessage(), null);
        } catch (AuthorizeException e) {
            context.abort();
            LOGGER.error("Something went wrong while searching for authority person, AuthorizeException. Message: " + e.getMessage(), null);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } finally {
            processFinally(context);
        }

        LOGGER.debug("Authority person were successfully searched.");

        if (authorityPersons == null) {
            LOGGER.debug("Authority persons not found with name=" + name + ".");
        } else {
            LOGGER.debug("Found " + authorityPersons.length + " authority persons with name=" + name + ".");
        }
        return authorityPersons;
    }

    /**
     * Search for authority person by uid in database.
     *
     * @param context Context for searching in database.
     * @param uid     Uid of authority person.
     *
     * @return Returns authority persons if exists. Otherwise throws exception with NOT_FOUND.
     *
     * @throws WebApplicationException Is throw when authority person is not found. Or was problem with reading from
     *                                 database.
     */
    private org.dspace.authority.common.AuthorityPerson searchForAuthorityPersonByUid(final org.dspace.core.Context context, final String uid) throws WebApplicationException {
        org.dspace.authority.common.AuthorityPerson person = null;

        try {
            person = org.dspace.authority.common.AuthorityPerson.findByUID(context, uid);

            if (person == null) {
                context.abort();
                LOGGER.warn("Authority person(uid=" + uid + ") was not found!");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

        } catch (SQLException e) {
            processException("Something get wrong while finding authority person(uid=" + uid + "). SQLException, Message: " + e, context);
        }

        return person;
    }

    /**
     * Checks if user logged into context has permission to change authority things.
     *
     * @param context Context, in which is logged user.
     *
     * @throws WebApplicationException Is throw when user has no permission to do that.
     */
    private void checkAuthorization(final org.dspace.core.Context context) throws WebApplicationException {
        final EPerson person = context.getCurrentUser();
        if (person == null || !userIsAdmin(context, person)) {
            context.abort();
            LOGGER.debug("user has not permission to do something with authority persons.");
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
    }

    /**
     * Check if user is admin.
     *
     * @param context Context to be able search for group.
     * @param person  Person which will tested.
     *
     * @return Returns true if user is admin, otherwise false.
     */
    private boolean userIsAdmin(final org.dspace.core.Context context, final EPerson person) {
        boolean admin = false;

        final Group group;
        try {
            group = Group.find(context, Group.ADMIN_ID);
            if (group != null) {
                for (final EPerson groupPerson : group.getMembers()) {
                    if (groupPerson.getEmail().equals(person.getEmail())) {
                        admin = true;
                    }
                }
            }
        } catch (SQLException e) {
            processException("Something get wrong while reading group from database. SQLException, Message: " + e, context);
        }

        return admin;
    }

    /**
     * Look for authority in authority person.
     *
     * @param dspaceAuthorityPerson Dspace authority person in which will be authority looked for.
     * @param nameOfAuthority       Name of authority in authority person.
     *
     * @return Return authority if is in authority person. Otherwise throw exception with NOT_FOUND.
     *
     * @throws SQLException       Is thrown when was problem with reading authorities.
     * @throws AuthorizeException If user has not permission.
     */
    private org.dspace.authority.common.Authority getAuthorityFromPerson(
            final org.dspace.core.Context context,
            final org.dspace.authority.common.AuthorityPerson dspaceAuthorityPerson,
            final String nameOfAuthority) throws SQLException, AuthorizeException {

        org.dspace.authority.common.Authority authority = null;
        final List<org.dspace.authority.common.Authority> dspaceAuthorities = dspaceAuthorityPerson.getAuthorities();
        for (final org.dspace.authority.common.Authority dspaceAuthority : dspaceAuthorities) {
            if (dspaceAuthority.getName().equals(nameOfAuthority)) {
                authority = dspaceAuthority;
            }
        }
        if (authority == null) {
            context.abort();
            LOGGER.warn("Authority not found in person(" + dspaceAuthorityPerson.getUid() + ").");
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        return authority;
    }

    /**
     * Check if string is not null and empty.
     *
     * @param text String to test.
     *
     * @return True if string is not empty or not null. Otherwise false.
     */
    private boolean stringNotEmpty(final String text) {
        return text != null && !text.isEmpty();
    }

    /**
     * Check if all strings are not null and not empty.
     *
     * @param strings Strings to tests.
     *
     * @return True if all strings are not empty or not null. Otherwise false.
     */
    private boolean stringsNotEmpty(final String... strings) {
        for (final String text : strings) {
            if (!stringNotEmpty(text)) {
                return false;
            }
        }
        return true;
    }
}
